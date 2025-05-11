# Iuliia Kapustinskaia Java Wrocław

Technical Screening Task for Ocado

## Build

```Bash
gradlew clean shadowJar
```

## Usage

```Bash
java -jar Iuliia_Kapustinskaia_Java_Wroclaw-all-1.0-SNAPSHOT.jar orders.json paymentmethods.json
```

## Features

- Parses input JSON files for:
    - **Orders** (`List<Order>`): each has `id`, `value`, optional `promotions` array.
    - **Payment Methods** (`List<PaymentMethod>`): each has `id`, `limit`, `discount` percentage.
- Applies a three-phase payment algorithm:
    1. Full payment by methods with >10% discount.
    2. Distribute PUNKTY (points) among remaining orders (10% minimum each, then proportional).
    3. Fallback payment for unpaid orders using the lowest-discount or full-price method.
- Outputs total paid per method and remaining limits.

