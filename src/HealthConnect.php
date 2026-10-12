<?php

namespace MarioDevv\HealthConnect;

use DateTimeImmutable;
use DateTimeInterface;
use DateTimeZone;

class HealthConnect
{
    public const TYPES = ['steps', 'distance', 'active_calories', 'heart_rate', 'weight', 'sleep', 'exercise'];

    /** @return array{available:bool, granted:list<string>} */
    public function status(): array
    {
        return $this->call('HealthConnect.Status');
    }

    /** Opens the system dialog. It is async: poll status() for the result. */
    public function requestPermissions(array $types = self::TYPES): array
    {
        return $this->call('HealthConnect.RequestPermissions', ['types' => array_values($types)]);
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
