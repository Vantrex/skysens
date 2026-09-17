Flyway migrations live here, named `V<n>__<description>.sql`.

Intentionally empty for this pass: §11 puts real persistence schemas out of scope,
and the entities under `server/entity/` carry no fields yet. The first migration
should be written together with the first entity that gains them, never generated
from `ddl-auto`.
