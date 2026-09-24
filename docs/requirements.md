# Prices Service — Requirements

| | |
|---|---|
| **Status** | Draft |
| **Last updated** | 2026-09-24 |
| **Sources** | Original technical-test statement (transcribed in Appendix A) · Seed data [`source/prices.csv`](source/prices.csv) · Additional reviewer instruction (section 4.2) |
| **Related documents** | [`architecture.md`](architecture.md): how the service is built |

This document defines **what** the service must do and the constraints it must respect. Design and
implementation choices belong in the related documents. If they disagree with this one, this one wins.

## 1. Context

The company's e-commerce database has a `PRICES` table. Each row holds the final retail price (PVP)
and the price list that applies to a product of a brand between two dates. The service answers one
question: *which price applies to this product of this brand at this moment?*

## 2. Glossary

| Term              | Spanish (original)  | Meaning                                                                                      |
|-------------------|---------------------|----------------------------------------------------------------------------------------------|
| Brand             | Cadena              | A store chain of the group, identified by `BRAND_ID`. Brand `1` is referred to as STORE Z.   |
| Product           | Producto            | An item sold by a brand, identified by `PRODUCT_ID`.                                         |
| Price list        | Tarifa              | An identified price that applies to a product of a brand within a date range (`PRICE_LIST`). |
| Final price (PVP) | Precio final (pvp)  | The retail sale price (`PRICE`).                                                             |
| Priority          | Prioridad           | Disambiguator. When several price lists apply at the same moment, the highest value wins.    |
| Application date  | Fecha de aplicación | The date and time for which the applicable price is requested.                               |
| Applicable price  | Precio a aplicar    | The single price selected for a brand, product and application date (FR-4).                  |

## 3. Data model

### 3.1 Entity `PRICES`

The original statement allows renaming fields, adding new ones and choosing the data types. The
types below are logical. The physical SQL and Java types are defined in `architecture.md`.

| Column           | CSV header     | Description                                                    | Logical type                                   | Constraints               |
|------------------|----------------|----------------------------------------------------------------|------------------------------------------------|---------------------------|
| `ID`             | —              | Surrogate identifier. Added, not in the source data.           | Integer identifier, generated                  | Unique, required          |
| `BRAND_ID`       | `BrandId`      | Brand the price belongs to.                                    | Integer identifier                             | Required                  |
| `START_DATE`     | `StartDate`    | Start of the range in which the price applies (inclusive, D3). | Date-time, no time zone, second precision      | Required                  |
| `END_DATE`       | `EndDate`      | End of the range in which the price applies (inclusive, D3).   | Date-time, no time zone, second precision      | Required, `>= START_DATE` |
| `PRICE_LIST`     | `PriceList`    | Identifier of the price list.                                  | Integer identifier                             | Required                  |
| `PRODUCT_ID`     | `ProductId`    | Product the price belongs to.                                  | Integer identifier                             | Required                  |
| `PRIORITY`       | `Priority`     | Disambiguator. The highest value wins (FR-4).                  | Integer                                        | Required, `>= 0`          |
| `PRICE`          | `Price`        | Final sale price.                                              | Exact decimal, 2 fraction digits               | Required, `>= 0`          |
| `CURR`           | `Currency`     | Currency of the price.                                         | ISO 4217 alphabetic code (3 uppercase letters) | Required                  |
| `LAST_UPDATE`    | `LastUpdate`   | Audit: when the row was last modified.                         | Date-time, no time zone, second precision      | Required                  |
| `LAST_UPDATE_BY` | `LastUpdateBy` | Audit: user who last modified the row.                         | Text, up to 50 characters                      | Required                  |

- **Money:** prices are exact decimals, never floating point (D11).
- **Audit columns:** stored, but not exposed by the API.
- **Brand and product:** there are no master tables for them. Their ids are plain values (D9).

### 3.2 Seed data

The database must be initialised with exactly these rows. The source is
[`source/prices.csv`](source/prices.csv), and the rows match the example in the original statement.
The `#` column is only a reference used in section 5.

| # | BRAND_ID | START_DATE          | END_DATE            | PRICE_LIST | PRODUCT_ID | PRIORITY | PRICE | CURR | LAST_UPDATE         | LAST_UPDATE_BY |
|---|----------|---------------------|---------------------|------------|------------|----------|-------|------|---------------------|----------------|
| 1 | 1        | 2020-06-14 00:00:00 | 2020-12-31 23:59:59 | 1          | 35455      | 0        | 35.50 | EUR  | 2020-03-26 14:49:07 | user1          |
| 2 | 1        | 2020-06-14 15:00:00 | 2020-06-14 18:30:00 | 2          | 35455      | 1        | 25.45 | EUR  | 2020-05-26 15:38:22 | user1          |
| 3 | 1        | 2020-06-15 00:00:00 | 2020-06-15 11:00:00 | 3          | 35455      | 1        | 30.50 | EUR  | 2020-05-26 15:39:22 | user2          |
| 4 | 1        | 2020-06-15 16:00:00 | 2020-12-31 23:59:59 | 4          | 35455      | 1        | 38.95 | EUR  | 2020-06-02 10:14:00 | user1          |

The source writes dates as `yyyy-MM-dd-HH.mm.ss` (e.g. `2020-06-14-00.00.00`). See D1.

## 4. Requirements

### 4.1 Functional requirements (original statement)

- **FR-1 — Price query endpoint.** Provide a Spring Boot service that exposes a REST query endpoint.
- **FR-2 — Input.** The endpoint accepts an application date (date and time), a product identifier
  and a brand identifier.
- **FR-3 — Output.** The endpoint returns the product identifier, the brand identifier, the price list
  to apply, the application dates (start and end of the selected price's range, D7) and the final
  price to apply. It also returns the currency (D8).
- **FR-4 — Price selection.** From the prices of the given brand and product whose date range
  contains the application date (D3), the one with the highest priority applies. Ties are broken by
  D4. If none applies, see D5.
- **FR-5 — Persistence.** Use an in-memory database (H2 type), initialised with the seed data in section 3.2.
- **FR-6 — Endpoint tests.** Provide tests against the REST endpoint that validate the acceptance
  criteria in section 5.1 with the seed data.

### 4.2 Additional requirements (reviewer instruction)

> Aplica correctamente la arquitectura hexagonal (infrastructure, application y domain) y haz test
> unitarios de los casos de uso y de streams para resolver los algoritmos.

- **AR-1 — Hexagonal architecture.** Apply hexagonal architecture correctly, with three layers:
  `domain`, `application` and `infrastructure`. See C1–C6.
- **AR-2 — Use case unit tests.** Unit-test the use cases, isolated from the framework and the database. See T3.
- **AR-3 — Streams for the algorithms.** Implement the business algorithms (finding the applicable
  prices and choosing the winner) with the Java Stream API, and unit-test them. See C5 and T2.

## 5. Acceptance criteria

All cases use the seed data, brand `1` and product `35455` unless noted. "Rows" refers to the `#`
column in section 3.2.

### 5.1 Required cases (original statement)

The original statement defines these requests. They are all in June 2020 (D12). It does not give the
expected results. The results below come from applying FR-4 to the seed data.

| ID   | Application date    | Candidate rows | Price list | Price     | Range                                     |
|------|---------------------|----------------|------------|-----------|-------------------------------------------|
| AT-1 | 2020-06-14 10:00:00 | 1              | 1          | 35.50 EUR | 2020-06-14 00:00:00 → 2020-12-31 23:59:59 |
| AT-2 | 2020-06-14 16:00:00 | 1, 2           | 2          | 25.45 EUR | 2020-06-14 15:00:00 → 2020-06-14 18:30:00 |
| AT-3 | 2020-06-14 21:00:00 | 1              | 1          | 35.50 EUR | 2020-06-14 00:00:00 → 2020-12-31 23:59:59 |
| AT-4 | 2020-06-15 10:00:00 | 1, 3           | 3          | 30.50 EUR | 2020-06-15 00:00:00 → 2020-06-15 11:00:00 |
| AT-5 | 2020-06-16 21:00:00 | 1, 4           | 4          | 38.95 EUR | 2020-06-15 16:00:00 → 2020-12-31 23:59:59 |

### 5.2 Boundary and error cases

| ID  | Request                                            | Candidate rows | Expected result          | What it checks                                   |
|-----|----------------------------------------------------|----------------|--------------------------|--------------------------------------------------|
| B1  | 2020-06-14 00:00:00                                | 1              | 200, price list 1, 35.50 | Start of range is inclusive (D3)                 |
| B2  | 2020-06-14 18:30:00                                | 1, 2           | 200, price list 2, 25.45 | End of range is inclusive (D3)                   |
| B3  | 2020-06-14 18:30:01                                | 1              | 200, price list 1, 35.50 | A range stops applying one second after its end  |
| B4  | 2020-06-15 11:00:00                                | 1, 3           | 200, price list 3, 30.50 | End of range is inclusive (D3)                   |
| B5  | 2020-06-15 11:00:01                                | 1              | 200, price list 1, 35.50 | A range stops applying one second after its end  |
| B6  | 2020-06-15 16:00:00                                | 1, 4           | 200, price list 4, 38.95 | Start of range is inclusive (D3)                 |
| B7  | 2020-12-31 23:59:59                                | 1, 4           | 200, price list 4, 38.95 | Last second of the overlapping ranges            |
| B8  | 2020-06-13 23:59:59                                | —              | 404                      | Before any range (D5)                            |
| B9  | 2021-01-01 00:00:00                                | —              | 404                      | After every range (D5)                           |
| B10 | 2020-06-14 10:00:00, product `99999`               | —              | 404                      | Unknown product (D9)                             |
| B11 | 2020-06-14 10:00:00, brand `2`                     | —              | 404                      | Unknown brand (D9)                               |
| B12 | 2020-06-14 10:00:00.500                            | —              | 400                      | Fractional seconds are rejected (D13)            |
| B13 | Missing parameter, malformed date, non-positive id | —              | 400                      | Input validation (D6)                            |

## 6. Evaluation criteria (original statement)

- Design and construction of the service.
- Code quality.
- Correct test results.

## 7. Decisions and constraints

### 7.1 Technical stack
- Java 21, Spring Boot 4.1.x, Gradle.
- Spring Web MVC, Spring Data JPA, Bean Validation.
- H2 in-memory database.
- OpenAPI documentation.

### 7.2 API contract

```
GET /api/v1/prices?applicationDate={yyyy-MM-ddTHH:mm:ss}&productId={id}&brandId={id}
```

Example:

```
GET /api/v1/prices?applicationDate=2020-06-14T10:00:00&productId=35455&brandId=1
```

**200 OK**
```json
{
  "productId": 35455,
  "brandId": 1,
  "priceList": 1,
  "startDate": "2020-06-14T00:00:00",
  "endDate": "2020-12-31T23:59:59",
  "price": 35.50,
  "currency": "EUR"
}
```

Errors are returned as RFC 9457 `application/problem+json`. The status rules are D5, D6 and D13.
Unexpected errors return 500 without exposing internal details.

### 7.3 Decisions

Points the original statement leaves open, and the decision taken:

| ID  | Topic                                                             | Decision                                                                                                               |
|-----|-------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------|
| D1  | Non-standard date format in the source (`2020-06-14-00.00.00`)    | It is only how the source is written. Internally and in the API, dates use ISO-8601 (`2020-06-14T00:00:00`).          |
| D2  | Time zone not specified                                           | Dates have no time zone and are taken as the brand's local time.                                                      |
| D3  | Range limits                                                      | Both ends are inclusive: `START_DATE <= applicationDate <= END_DATE`.                                                 |
| D4  | Several applicable prices with the same highest priority          | The one with the latest `START_DATE` wins, so there is always one result. (Pending confirmation: Q1.)                 |
| D5  | No applicable price                                               | `404 Not Found`.                                                                                                       |
| D6  | Missing parameter, wrong type, malformed date, or id not positive | `400 Bad Request`.                                                                                                     |
| D7  | Meaning of "application dates" in the output                      | The `START_DATE` and `END_DATE` of the selected price.                                                                |
| D8  | Currency is not requested in the output                           | It is included, because a price without its currency is incomplete.                                                   |
| D9  | Brand and product existence can't be checked (no master tables)   | They are not checked. An unknown brand or product has no price, so D5 applies.                                        |
| D10 | HTTP method and parameter style                                   | `GET` with query parameters: a read-only, idempotent query.                                                           |
| D11 | Numeric representation of money                                   | Exact decimal with 2 fraction digits, never floating point.                                                           |
| D12 | The statement gives only the day ("día 14") for the test requests | June 2020, the only month consistent with the seed data.                                                              |
| D13 | Precision of `applicationDate`                                    | Whole seconds (`yyyy-MM-ddTHH:mm:ss`). Fractional seconds are rejected with 400, because the data has second precision. |

### 7.4 Architecture constraints (AR-1, AR-3)
- **C1 — Three layers:** `domain`, `application`, `infrastructure`.
- **C2 — Dependencies point inwards only:** `infrastructure → application → domain`.
- **C3 — Framework-free core:** `domain` and `application` contain no framework, persistence or HTTP code.
- **C4 — Ports:** the use case is exposed through an inbound port. Persistence is reached through an
  outbound port, implemented by an adapter in `infrastructure`.
- **C5 — Business rule in the domain:** the price selection rule (FR-4, D3, D4) is implemented in the
  domain with the Java Stream API. The database may pre-filter candidates for efficiency, but it must
  not decide which price wins.
- **C6 — No leaking models:** persistence and API models never cross into the domain.

### 7.5 Testing requirements (FR-6, AR-2, AR-3)
- **T1 — Integration tests** for AT-1–AT-5 and B1–B13. They run against the full running application
  and the real seed data, with no layer mocked, and assert every response field. Mocked slice tests
  are extra and don't count in their place.
- **T2 — Domain unit tests** for the stream-based selection algorithm. They cover: no candidates, a
  single match, the highest priority winning, the tie-break (D4), non-applicable candidates ignored,
  and inclusive boundaries (D3).
- **T3 — Use case unit tests,** with the outbound port mocked and no framework context: the price is
  found, or nothing applies (D5).
- **T4 — Architecture test** that enforces C2 and C3 automatically.

### 7.6 Non-functional
- **N1:** the build compiles and passes all tests with no external services.
- **N2:** the API is documented with OpenAPI, and an interactive UI is available.
- **N3:** a README explains how to run the service and the tests, with example requests.

## 8. Open questions

| ID | Question                                                                                  | Current assumption |
|----|-------------------------------------------------------------------------------------------|--------------------|
| Q1 | Is a same-priority overlap valid data, or should it be treated as a data-integrity error? | Valid; D4 applies. |

## 9. Out of scope

- Creating, updating or deleting prices. The service is read-only.
- Authentication and authorisation.
- Brand and product master data (D9).
- Brands or products other than those in the seed data. The design must not prevent them, though.

---

## Appendix A. Original statement (Spanish)

Transcribed from the original statement. The only change is the brand name, which is replaced by
"STORE Z".

> En la base de datos de comercio electrónico de la compañía disponemos de la tabla PRICES que refleja el precio final (pvp) y la tarifa que aplica a un producto de una cadena entre unas fechas determinadas. A continuación, se muestra un ejemplo de la tabla con los campos relevantes:
>
> ```
> BRAND_ID  START_DATE           END_DATE             PRICE_LIST  PRODUCT_ID  PRIORITY  PRICE  CURR
> 1         2020-06-14-00.00.00  2020-12-31-23.59.59  1           35455       0         35.50  EUR
> 1         2020-06-14-15.00.00  2020-06-14-18.30.00  2           35455       1         25.45  EUR
> 1         2020-06-15-00.00.00  2020-06-15-11.00.00  3           35455       1         30.50  EUR
> 1         2020-06-15-16.00.00  2020-12-31-23.59.59  4           35455       1         38.95  EUR
> ```
>
> Campos:
> - BRAND_ID: foreign key de la cadena del grupo (1 = STORE Z).
> - START_DATE, END_DATE: rango de fechas en el que aplica el precio tarifa indicado.
> - PRICE_LIST: Identificador de la tarifa de precios aplicable.
> - PRODUCT_ID: Identificador código de producto.
> - PRIORITY: Desambiguador de aplicación de precios. Si dos tarifas coinciden en un rago de fechas se aplica la de mayor prioridad (mayor valor numérico).
> - PRICE: precio final de venta.
> - CURR: iso de la moneda.
>
> Se pide:
> - Construir una aplicación/servicio en SpringBoot que provea una end point rest de consulta tal que:
>   - Acepte como parámetros de entrada: fecha de aplicación, identificador de producto, identificador de cadena.
>   - Devuelva como datos de salida: identificador de producto, identificador de cadena, tarifa a aplicar, fechas de aplicación y precio final a aplicar.
>
> Se debe utilizar una base de datos en memoria (tipo h2) e inicializar con los datos del ejemplo, (se pueden cambiar el nombre de los campos y añadir otros nuevos si se quiere, elegir el tipo de dato que se considere adecuado para los mismos).
>
> - Desarrollar unos test al endpoint rest que validen las siguientes peticiones al servicio con los datos del ejemplo:
>   - Test 1: petición a las 10:00 del día 14 del producto 35455 para la brand 1 (STORE Z)
>   - Test 2: petición a las 16:00 del día 14 del producto 35455 para la brand 1 (STORE Z)
>   - Test 3: petición a las 21:00 del día 14 del producto 35455 para la brand 1 (STORE Z)
>   - Test 4: petición a las 10:00 del día 15 del producto 35455 para la brand 1 (STORE Z)
>   - Test 5: petición a las 21:00 del día 16 del producto 35455 para la brand 1 (STORE Z)
>
> Se valorará:
> - Diseño y construcción del servicio.
> - Calidad de Código.
> - Resultados correctos en los test.
