package com.mariodevv.plugins.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object HealthConnectCore {
    val types = listOf("steps", "distance", "active_calories", "heart_rate", "weight", "sleep", "exercise")

    // ponytail: hard cap per Read call; add pagination to the PHP API if anyone needs more.
    private const val MAX_RECORDS = 10_000

    fun permission(type: String): String? = when (type) {
        "steps" -> HealthPermission.getReadPermission(StepsRecord::class)
        "distance" -> HealthPermission.getReadPermission(DistanceRecord::class)
        "active_calories" -> HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        "heart_rate" -> HealthPermission.getReadPermission(HeartRateRecord::class)
        "weight" -> HealthPermission.getReadPermission(WeightRecord::class)
        "sleep" -> HealthPermission.getReadPermission(SleepSessionRecord::class)
        "exercise" -> HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        else -> null
    }

    fun available(context: Context) =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private suspend fun grantedTypes(client: HealthConnectClient): List<String> {
        val granted = client.permissionController.getGrantedPermissions()
        return types.filter { permission(it) in granted }
    }

    suspend fun status(context: Context): JSONObject {
        val available = available(context)
        val granted = if (available) grantedTypes(HealthConnectClient.getOrCreate(context)) else emptyList()
        return JSONObject().put("available", available).put("granted", JSONArray(granted))
    }

    suspend fun read(context: Context, type: String, from: Instant, to: Instant): JSONObject {
        val client = connect(context, type)
        val range = TimeRangeFilter.between(from, to)
        val records = when (type) {
            "steps" -> readAll<StepsRecord>(client, range) {
                interval(it.startTime, it.endTime, it).put("count", it.count)
            }
            "distance" -> readAll<DistanceRecord>(client, range) {
                interval(it.startTime, it.endTime, it).put("meters", it.distance.inMeters)
            }
            "active_calories" -> readAll<ActiveCaloriesBurnedRecord>(client, range) {
                interval(it.startTime, it.endTime, it).put("kcal", it.energy.inKilocalories)
            }
            "heart_rate" -> readAll<HeartRateRecord>(client, range) { r ->
                interval(r.startTime, r.endTime, r).put("samples", JSONArray(r.samples.map {
                    JSONObject().put("time", it.time.toString()).put("bpm", it.beatsPerMinute)
                }))
            }
            "weight" -> readAll<WeightRecord>(client, range) {
                JSONObject().put("time", it.time.toString()).put("kg", it.weight.inKilograms)
                    .put("source", it.metadata.dataOrigin.packageName)
            }
            "sleep" -> readAll<SleepSessionRecord>(client, range) { r ->
                interval(r.startTime, r.endTime, r).put("title", r.title).put("stages", JSONArray(r.stages.map {
                    JSONObject().put("start", it.startTime.toString()).put("end", it.endTime.toString())
                        .put("stage", SleepSessionRecord.STAGE_TYPE_INT_TO_STRING_MAP[it.stage] ?: "unknown")
                }))
            }
            else -> readAll<ExerciseSessionRecord>(client, range) {
                interval(it.startTime, it.endTime, it).put("title", it.title)
                    .put("type", ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP[it.exerciseType] ?: "unknown")
            }
        }
        return JSONObject().put("type", type).put("records", records)
    }

    /** Health Connect dedupes overlapping sources here; summing Read output would double count. */
    suspend fun aggregate(context: Context, type: String, from: Instant, to: Instant): JSONObject {
        val client = connect(context, type)
        val range = TimeRangeFilter.between(from, to)
        val out = JSONObject().put("type", type)
        when (type) {
            "steps" -> out.put("count", agg(client, StepsRecord.COUNT_TOTAL, range) ?: 0L)
            "distance" -> out.put("meters", agg(client, DistanceRecord.DISTANCE_TOTAL, range)?.inMeters ?: 0.0)
            "active_calories" -> out.put("kcal", agg(client, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL, range)?.inKilocalories ?: 0.0)
            "sleep" -> out.put("seconds", agg(client, SleepSessionRecord.SLEEP_DURATION_TOTAL, range)?.seconds ?: 0L)
            "heart_rate" -> {
                val r = client.aggregate(AggregateRequest(
                    setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX), range))
                out.put("avg", r[HeartRateRecord.BPM_AVG]).put("min", r[HeartRateRecord.BPM_MIN]).put("max", r[HeartRateRecord.BPM_MAX])
            }
            else -> return JSONObject().put("error", "aggregate not supported for '$type'")
        }
        return out
    }

    private suspend fun <T : Any> agg(
        client: HealthConnectClient,
        metric: androidx.health.connect.client.aggregate.AggregateMetric<T>,
        range: TimeRangeFilter,
    ): T? = client.aggregate(AggregateRequest(setOf(metric), range))[metric]

    private suspend inline fun <reified T : Record> readAll(
        client: HealthConnectClient,
        range: TimeRangeFilter,
        map: (T) -> JSONObject,
    ): JSONArray {
        val out = JSONArray()
        var token: String? = null
        do {
            val page = client.readRecords(ReadRecordsRequest(T::class, range, pageToken = token))
            page.records.forEach { out.put(map(it)) }
            token = page.pageToken
        } while (token != null && out.length() < MAX_RECORDS)
        return out
    }

    private fun interval(start: Instant, end: Instant, r: Record) = JSONObject()
        .put("start", start.toString()).put("end", end.toString())
        .put("source", r.metadata.dataOrigin.packageName)

    /** Throws (reported as { error }) when the type is unknown, Health Connect is unavailable or the permission is missing. */
    private suspend fun connect(context: Context, type: String): HealthConnectClient {
        val perm = permission(type) ?: error("unknown type '$type'")
        if (!available(context)) error("unavailable")
        return HealthConnectClient.getOrCreate(context).also {
            if (perm !in it.permissionController.getGrantedPermissions()) error("permission_required")
        }
    }
}
