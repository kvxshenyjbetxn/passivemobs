# PassiveMobs

**Єдине завдання:** ворожі моби не агряться на гравця. Більше нічого.

Працює на сервері й клієнті, не ламає жодних механік, повністю сумісний з GTNH 1.7.10.

## Як працює

Усього **два Forge-івенти** (жодного ASM, жодних міксинів):

1. `LivingSetAttackTargetEvent` — скидає ціль моба в `null`, якщо це гравець
2. `LivingAttackEvent` — страхувальний fallback, скасовує шкоду від моба гравцю

Це все. Жодного втручання в AI, спавн, лутабо інші системи.

## Конфіг

```ini
# config/PassiveMobs.cfg
general {
    B:enabled=true   # false = моди знову ворожі
}
```

## Команди (OP level 2)

```
/passivemobs          — статус
/passivemobs on       — увімкнути
/passivemobs off      — вимкнути
/passivemobs reload   — перечитати конфіг
```

## Білд

```bash
gradle clean build
# → build/libs/passivemobs-1.0.0.jar
```

## Сумісність

- Minecraft 1.7.10 / Forge 10.13.4.1614
- GTNH 2.x (будь-яка версія)
- Java 8 — Java 25
- Сервер / клієнт / singleplayer
