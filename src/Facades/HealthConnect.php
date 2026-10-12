<?php

namespace MarioDevv\HealthConnect\Facades;

use Illuminate\Support\Facades\Facade;

/**
 * @method static array status()
 * @method static array requestPermissions(array $types = [], bool $write = false)
 * @method static array write(string $type, array $data)
 * @method static array read(string $type, \DateTimeInterface|string $from, \DateTimeInterface|string $to)
 * @method static array aggregate(string $type, \DateTimeInterface|string $from, \DateTimeInterface|string $to)
 *
 * @see \MarioDevv\HealthConnect\HealthConnect
 */
class HealthConnect extends Facade
{
    protected static function getFacadeAccessor(): string
    {
        return \MarioDevv\HealthConnect\HealthConnect::class;
    }
}
