-- =====================================================================
-- Esquema Supabase - Estadio Inteligente (Lab-1 / Avance)
-- Ejecutar desde el SQL Editor de Supabase (Project > SQL > New query).
-- =====================================================================

-- Lecturas de sensores (HU-01, HU-02, HU-03).
create table if not exists public.sensor_readings (
    id                bigserial primary key,
    read_at           timestamptz not null default now(),
    entry_count       integer     not null,
    distance_cm       real        not null,
    presence_detected boolean     not null,
    light_intensity   integer     not null,
    source            text        not null default 'arduino'
);

create index if not exists sensor_readings_read_at_idx
    on public.sensor_readings (read_at desc);

-- Eventos del sistema (cambios de modo, alarmas, luces, umbrales).
create table if not exists public.events (
    id           bigserial primary key,
    occurred_at  timestamptz not null default now(),
    kind         text        not null,
    payload      jsonb       not null default '{}'::jsonb
);

create index if not exists events_occurred_at_idx
    on public.events (occurred_at desc);
create index if not exists events_kind_idx
    on public.events (kind);

-- Configuracion vigente del estadio (HU-12). Una sola fila.
create table if not exists public.stadium_config (
    id                   integer primary key default 1,
    occupancy_threshold  integer not null default 100,
    distance_threshold   real    not null default 30.0,
    updated_at           timestamptz not null default now(),
    constraint stadium_config_singleton check (id = 1)
);

insert into public.stadium_config (id) values (1)
on conflict (id) do nothing;

-- =====================================================================
-- Seguridad (RLS)
-- Para una app con service_role key basta con dejar RLS habilitado sin
-- policies (service_role las salta). Si se usa anon key conviene definir
-- policies explicitas segun el entorno.
-- =====================================================================
alter table public.sensor_readings enable row level security;
alter table public.events          enable row level security;
alter table public.stadium_config  enable row level security;
