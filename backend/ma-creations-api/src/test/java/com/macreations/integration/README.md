# Catalog API — MySQL integration tests (not run by default)

These tests require a running MySQL instance with Flyway migrations applied.

They are **not** included as automatic CI tests in this step because MySQL is not
guaranteed in every developer environment.

To add later (STEP 7+):

- `@SpringBootTest` + `@AutoConfigureMockMvc`
- Profile `integration` with real `DB_*` credentials
- Assert seeded categories and create/list/get product against MySQL

Until then, catalog behavior is covered by:

- `CatalogUtilsTest` (unit)
- `CategoryServiceTest` / `ProductServiceTest` (unit, mocked repositories)
- `CatalogControllerTest` (`@WebMvcTest`, mocked services)
