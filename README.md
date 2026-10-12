# Health Connect for NativePHP Mobile

Read Android [Health Connect](https://developer.android.com/health-and-fitness/health-connect) data from PHP: steps, distance, active calories, heart rate, weight, sleep and exercise sessions. Includes deduplicated totals (`aggregate`) so overlapping sources (phone + watch) are not double counted.

Android only. iOS HealthKit is not supported.

## Requirements

- NativePHP Mobile 3.x or 4.x
- Android 14+ (API 34). In your app's `.env`:

```
NATIVEPHP_ANDROID_MIN_SDK=34
```

Health Connect ships with Android 14. On older versions it is a separate app that this plugin cannot detect, so they are not supported.

## Install

```bash
composer require mariodevv/nativephp-health-connect
php artisan native:plugin:register mariodevv/nativephp-health-connect
php artisan native:run
```

## Usage

```php
use MarioDevv\HealthConnect\Facades\HealthConnect;

// 1. Ask the user. Opens the system dialog and returns immediately.
HealthConnect::requestPermissions(['steps', 'sleep']);   // default: all types

// 2. Check what was granted (poll, e.g. on page load).
HealthConnect::status();
// ['available' => true, 'granted' => ['steps', 'sleep'], 'writable' => []]

// 3. Totals (deduplicated across sources).
HealthConnect::aggregate('steps', now()->subDay(), now());
// ['type' => 'steps', 'count' => 8423]

// 4. Raw records.
HealthConnect::read('heart_rate', now()->subDay(), now());
// ['type' => 'heart_rate', 'records' => [['start' => ..., 'end' => ..., 'source' => 'com.fitbit.FitbitMobile', 'samples' => [['time' => ..., 'bpm' => 64], ...]]]]
```

`$from` / `$to` accept `DateTimeInterface` or any string `DateTimeImmutable` parses. Times are returned as UTC ISO-8601.

### Writing (1.1)

Ask for write access on top of read access with `requestPermissions(['steps', 'weight'], write: true)`, then:

```php
HealthConnect::write('steps', ['count' => 1200, 'from' => now()->subMinutes(30), 'to' => now()]);
HealthConnect::write('weight', ['kg' => 72.5]);          // 'time' defaults to now
// ['type' => 'weight', 'written' => 1]
```

Only `steps` and `weight` can be written. `status()['writable']` lists what the user granted. Health Connect applies its own source priority when reading totals, so steps you write may be dropped from `aggregate('steps')` where the phone's own step counter overlaps in time.

Errors come back as `['error' => '...']`: `permission_required`, `unavailable`, `unknown type '...'`, `write not supported for '...'`.

### Types

| Type | `read` record fields | `aggregate` |
|---|---|---|
| `steps` | `start`, `end`, `count`, `source` | `count` |
| `distance` | `start`, `end`, `meters`, `source` | `meters` |
| `active_calories` | `start`, `end`, `kcal`, `source` | `kcal` |
| `heart_rate` | `start`, `end`, `samples[{time,bpm}]`, `source` | `avg`, `min`, `max` |
| `weight` | `time`, `kg`, `source` | not supported |
| `sleep` | `start`, `end`, `title`, `stages[{start,end,stage}]`, `source` | `seconds` |
| `exercise` | `start`, `end`, `title`, `type`, `source` | not supported |

## Limits

- `read` returns at most 10,000 records per call. Narrow the time range or use `aggregate`.
- Writing is limited to steps and weight.
- Sleep `stages` is implemented but not yet tested against a real device that records stages (tested with sessions without stages).
- Health Connect only returns data the user has granted, and only data written by other apps or devices.

## License

MIT
