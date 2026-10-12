<?php

namespace MarioDevv\HealthConnect;

use DateTimeImmutable;
use DateTimeInterface;
use DateTimeZone;

class HealthConnect
{
    public const TYPES = ['steps', 'distance', 'active_calories', 'heart_rate', 'weight', 'sleep', 'exercise'];

    /** @return array{available:bool, granted:list<string>, writable:list<string>} */
    public function status(): array
    {
        return $this->call('HealthConnect.Status');
    }

    /** Opens the system dialog. It is async: poll status() for the result. $write also asks write access (steps, weight). */
    public function requestPermissions(array $types = self::TYPES, bool $write = false): array
    {
        return $this->call('HealthConnect.RequestPermissions', ['types' => array_values($types), 'write' => $write]);
    }

    /**
     * Writes one record. steps: ['count' => 1200, 'from' => ..., 'to' => ...]. weight: ['kg' => 72.5, 'time' => ...] (time defaults to now).
     *
     * @return array{type:string, written:int}|array{error:string}
     */
    public function write(string $type, array $data): array
    {
        foreach (['from', 'to', 'time'] as $k) {
            if (isset($data[$k])) {
                $data[$k] = $this->iso($data[$k]);
            }
        }

        return $this->call('HealthConnect.Write', ['type' => $type, ...$data]);
    }

    /** @return array{type:string, records:list<array>}|array{error:string} */
    public function read(string $type, DateTimeInterface|string $from, DateTimeInterface|string $to): array
    {
        return $this->call('HealthConnect.Read', ['type' => $type, 'from' => $this->iso($from), 'to' => $this->iso($to)]);
    }

    /** Deduplicated total, e.g. ['type' => 'steps', 'count' => 8423]. */
    public function aggregate(string $type, DateTimeInterface|string $from, DateTimeInterface|string $to): array
    {
        return $this->call('HealthConnect.Aggregate', ['type' => $type, 'from' => $this->iso($from), 'to' => $this->iso($to)]);
    }

    private function iso(DateTimeInterface|string $t): string
    {
        $t = $t instanceof DateTimeInterface ? DateTimeImmutable::createFromInterface($t) : new DateTimeImmutable($t);

        return $t->setTimezone(new DateTimeZone('UTC'))->format('Y-m-d\TH:i:s\Z');
    }

    private function call(string $name, array $params = []): array
    {
        if (! function_exists('nativephp_call')) {
            return ['error' => 'unsupported'];
        }

        $raw = json_decode(nativephp_call($name, json_encode($params)) ?: '[]', true);

        return json_decode($raw['json'] ?? '[]', true) ?: ['error' => $raw['error'] ?? 'bridge returned nothing'];
    }
}
