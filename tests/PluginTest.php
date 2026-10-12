<?php

// Manifest ↔ native code contract + permission/type coverage. Run: composer install && ./vendor/bin/pest

it('keeps manifest, Kotlin, PHP and permissions in sync', function () {
    $root = dirname(__DIR__);
    $manifest = json_decode(file_get_contents("$root/nativephp.json"), true);
    $kotlin = implode('', array_map('file_get_contents', glob("$root/resources/android/src/com/mariodevv/plugins/healthconnect/*.kt")));
    $php = file_get_contents("$root/src/HealthConnect.php");

    foreach ($manifest['bridge_functions'] as $fn) {
        expect($kotlin)->toContain('class '.last(explode('.', $fn['android'])));
        expect($php)->toContain("'{$fn['name']}'");
    }

    // every PHP type has a Kotlin permission branch, and a declared READ_* permission
    preg_match("/TYPES = \[(.+?)\]/", $php, $m);
    $types = array_map(fn ($t) => trim($t, " '"), explode(',', $m[1]));
    $reads = array_filter($manifest['android']['permissions'], fn ($x) => str_contains($x, '.READ_'));
    expect($types)->toHaveCount(count($reads));
    foreach ($types as $t) {
        expect($kotlin)->toContain("\"$t\" ->");
    }
});
