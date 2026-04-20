# Integracion con Supabase

Este directorio contiene todo lo necesario para conectar el sistema de
estadio inteligente a una base de datos Supabase.

## 1. Crear el proyecto

1. Entrar a https://supabase.com y crear un nuevo proyecto.
2. Anotar el **Project URL** (`https://xxxx.supabase.co`) y la
   **API key** (`Project Settings` -> `API`).
   - `anon` key: publica, se usa con Row Level Security.
   - `service_role` key: privilegiada, salta RLS. Recomendada para el
     proceso Java que corre localmente.

## 2. Crear las tablas

Abrir el **SQL Editor** en el dashboard de Supabase y ejecutar el
contenido de [`schema.sql`](./schema.sql). Crea tres tablas:

| Tabla             | Proposito                                           |
|-------------------|-----------------------------------------------------|
| `sensor_readings` | Historico de lecturas del Arduino (HU-01 / HU-02).  |
| `events`          | Eventos del sistema (alarma, luces, modos, umbral). |
| `stadium_config`  | Configuracion vigente (singleton, `id = 1`).        |

## 3. Configurar credenciales

Copiar el template y rellenarlo:

```bash
cp Avance/supabase/.env.example Avance/supabase/.env
```

Editar `Avance/supabase/.env`:

```env
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_KEY=eyJhbGciOi...
```

Tambien funciona exportando las variables de entorno directamente:

```bash
export SUPABASE_URL=...
export SUPABASE_KEY=...
```

Si no hay credenciales, la aplicacion arranca igual pero sin
persistencia (usa `NoOpSensorRepository`).

## 4. Compilar y ejecutar

Desde la carpeta `Avance/`:

```bash
# Compilar
javac -d out -cp "lib/jSerialComm-2.11.0.jar" $(find src -name '*.java')

# Ejecutar
java -cp "out:lib/jSerialComm-2.11.0.jar" Main
```

En Windows usar `;` como separador en `-cp`.

Al arrancar veras una linea como:

```
  [SUPABASE] Conectando a https://tu-proyecto.supabase.co
```

## 5. Como fluye la informacion

- `StadiumFacade.readAllSensors()` -> inserta una fila en
  `sensor_readings` por cada ciclo de lectura.
- `StadiumFacade.setAlarm(on)` -> inserta evento `ALARM` con `{"on":...}`.
- `StadiumFacade.setLight(i)` -> inserta evento `LIGHT` con `{"intensity":i}`.
- `StadiumFacade.changeMode(m)` -> inserta evento `MODE` con `{"name":...}`.
- `StadiumFacade.setOccupancyThreshold(n)` y `setDistanceThreshold(cm)` ->
  hacen UPSERT de la fila singleton `stadium_config`.

Todas las escrituras son asincronas (`HttpClient.sendAsync`) para no
bloquear el ciclo de sensores si la red esta lenta.

## 6. Arquitectura

```
dominio/persistence/ISensorRepository   <-- puerto del dominio
        ^
        |
        +-- infraestructura/SupabaseRestRepository  (HTTP PostgREST)
        +-- infraestructura/NoOpSensorRepository    (fallback sin BD)
```

Esto mantiene el dominio desacoplado: cambiar Supabase por PostgreSQL
con JDBC, MongoDB, o un archivo CSV solo requiere una nueva
implementacion del puerto.

## 7. Consultas utiles

Ultimas 50 lecturas:

```sql
select * from public.sensor_readings
 order by read_at desc
 limit 50;
```

Aforo aproximado por hora:

```sql
select date_trunc('hour', read_at) as hora,
       max(entry_count) - min(entry_count) as flujo
  from public.sensor_readings
 group by 1
 order by 1 desc;
```

Eventos del dia:

```sql
select occurred_at, kind, payload
  from public.events
 where occurred_at > now() - interval '24 hours'
 order by occurred_at desc;
```
