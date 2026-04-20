# Historias de Usuario - Sistema de Control de Estadio Inteligente

## Hardware utilizado
- **Arduino Uno R3** - Microcontrolador Atmega328p - Tarjeta de Desarrollo
- **HC-SR312 AM312** - Mini PIR Sensor Movimiento Humano

---

## 1. Lectura de sensores (READ)

### HU-01 – Leer sensores
**Como** operador de consola,
**quiero** ejecutar el comando READ,
**para** obtener los datos actuales de los sensores del estadio.

**Criterios de aceptacion:**
- Muestra conteo de entradas
- Muestra nivel de luz
- Muestra deteccion de movimiento
- Funciona con hardware real o simulador

---

### HU-02 – Visualizar datos en consola
**Como** operador,
**quiero** ver los datos de sensores de forma clara,
**para** entender el estado del sistema en tiempo real.

**Criterios de aceptacion:**
- Datos formateados y legibles
- Actualizacion cada vez que se ejecuta READ

---

## 2. Conteo de entradas

### HU-03 – Contar entradas al estadio
**Como** sistema,
**quiero** contar cada persona que entra,
**para** mantener un registro de ocupacion.

**Criterios de aceptacion:**
- Incrementa contador al detectar paso
- No duplica conteos
- Se muestra en consola

---

## 3. Iluminacion

### HU-04 – Control manual de luces
**Como** operador,
**quiero** encender o apagar luces por zona,
**para** controlar la iluminacion manualmente.

**Criterios de aceptacion:**
- Comando: LIGHT ZONE_A ON/OFF
- Refleja el estado correctamente

---

### HU-05 – Iluminacion automatica
**Como** sistema,
**quiero** encender luces automaticamente si hay poca luz y presencia,
**para** optimizar visibilidad y consumo.

**Criterios de aceptacion:**
- Si luz baja + hay personas → luces ON
- Si no → luces OFF
- Solo en modo AUTO

---

## 4. Seguridad

### HU-06 – Activar alarma manualmente
**Como** operador,
**quiero** activar o desactivar la alarma,
**para** responder ante situaciones de riesgo.

**Criterios de aceptacion:**
- Comando: ALARM ON/OFF
- Se refleja en el sistema fisico

---

### HU-07 – Alarma por sobreocupacion
**Como** sistema,
**quiero** activar la alarma si se supera el limite de personas,
**para** garantizar la seguridad.

**Criterios de aceptacion:**
- Se activa cuando conteo > threshold
- Solo en modo AUTO o EMERGENCY

---

## 5. Modos de operacion

### HU-08 – Cambiar modo de operacion
**Como** operador,
**quiero** cambiar entre AUTO, MANUAL y EMERGENCY,
**para** controlar el comportamiento del sistema.

**Criterios de aceptacion:**
- Comando: MODE AUTO/MANUAL/EMERGENCY
- Cambia comportamiento del sistema

---

### HU-09 – Modo manual
**Como** operador,
**quiero** que el sistema solo responda a mis comandos,
**para** tener control total.

**Criterios de aceptacion:**
- No hay automatizacion
- Solo responde a comandos directos

---

### HU-10 – Modo automatico
**Como** sistema,
**quiero** ejecutar reglas automaticamente,
**para** optimizar operacion sin intervencion humana.

**Criterios de aceptacion:**
- Controla luces automaticamente
- Activa alarma por threshold

---

### HU-11 – Modo emergencia
**Como** sistema,
**quiero** priorizar la seguridad,
**para** reaccionar rapidamente ante riesgos.

**Criterios de aceptacion:**
- Alarma activa automaticamente
- Ignora algunas restricciones normales

---

## 6. Configuracion

### HU-12 – Configurar threshold de ocupacion
**Como** operador,
**quiero** definir el limite de personas,
**para** ajustar la sensibilidad del sistema de seguridad.

**Criterios de aceptacion:**
- Comando: SET THRESHOLD <valor>
- Se usa en logica de alarma

---

## 7. Hardware / Simulacion

### HU-13 – Usar hardware real
**Como** sistema,
**quiero** leer sensores reales desde Arduino,
**para** operar con el modelo fisico.

**Criterios de aceptacion:**
- Comunicacion serial funcional

---

### HU-14 – Usar simulador
**Como** desarrollador,
**quiero** simular sensores,
**para** poder probar sin hardware.

**Criterios de aceptacion:**
- Datos simulados consistentes
- Activable desde configuracion

---

## Alcance del laboratorio

Para este laboratorio se implementan las siguientes historias de usuario:
- **HU-01** – Leer sensores
- **HU-02** – Mostrar datos
- **HU-03** – Contar entradas
- **HU-13** – Usar hardware real
