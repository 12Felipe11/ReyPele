**Estadio Inteligente "Rey Pelé"**

**Descripción del Proyecto**

El proyecto "Estadio Inteligente Rey Pelé" consiste en el desarrollo de un sistema automatizado que integra hardware y software para la gestión eficiente de un estadio.

Se utiliza un Arduino Uno junto con diferentes sensores para recolectar información del entorno, la cual es procesada por una aplicación desarrollada en Java siguiendo principios de arquitectura por capas, programación orientada a objetos, SOLID y GRASP.

El sistema busca controlar el ingreso de personas, gestionar la iluminación y emitir alertas cuando se supere la capacidad máxima del estadio.

**Objetivos**

**Objetivo General**

Desarrollar un sistema inteligente para la gestión de un estadio mediante la integración de sensores físicos y una aplicación en Java.

**Objetivos Específicos**

Implementar el control de ingreso de personas mediante un sensor de proximidad.

Automatizar la iluminación utilizando un sensor de luz.

Generar alertas mediante un buzzer cuando se supere la capacidad del estadio.

Diseñar una arquitectura de software escalable y mantenible.

**Tecnologías Utilizadas**

**Hardware**

-Arduino Uno

-Sensor de proximidad

**Software**

-Java

-Comunicación serial (Arduino - Java)

-Programación Orientada a Objetos (POO)

-Arquitectura por capas

-Principios SOLID

-Patrones GRASP

**Arquitectura del Sistema**

El sistema se compone de dos partes principales:

**Sistema Embebido (Arduino)**

Encargado de:

-Leer los datos de los sensores

-Detectar eventos físicos (ingreso de personas, cambios de luz)

-Enviar información al sistema Java mediante comunicación serial

**Sistema de Software (Java)**

Encargado de:

-Procesar los datos recibidos desde Arduino

-Aplicar reglas de negocio

-Gestionar el estado del estadio

Generar respuestas del sistema (alertas, control de aforo)

**Arquitectura por Capas**

El sistema Java está estructurado de la siguiente manera:

**Capa de Presentación**

-Interfaz de usuario (consola o interfaz gráfica)
-Muestra la información del sistema

**Capa de Aplicación**

-Contiene los casos de uso

-Orquesta la ejecución de la lógica del sistema

**Capa de Dominio**

-Contiene la lógica de negocio

-Incluye entidades como Estadio y ControlAforo

**Capa de Infraestructura**

-Maneja la comunicación con Arduino
-Lectura de datos desde el puerto serial

**Principios de Diseño**

**SOLID**

Responsabilidad única: cada clase tiene una única función

Abierto/Cerrado: el sistema permite extensión sin modificar código existente

Sustitución de Liskov: uso correcto de herencia y abstracción

Segregación de interfaces: interfaces específicas para cada funcionalidad

Inversión de dependencias: desacoplamiento entre lógica de negocio y hardware

**GRASP**

Controller: manejo de eventos del sistema

Information Expert: las entidades gestionan su propia información

Low Coupling: bajo acoplamiento entre componentes

High Cohesion: alta cohesión en las clases

**Funcionamiento del Sistema**

El sensor de proximidad detecta el ingreso de una persona.

Arduino envía un mensaje a través del puerto serial.

La aplicación Java recibe el evento.

Se ejecuta el caso de uso correspondiente.

Se actualiza el número de personas dentro del estadio.

Se valida la capacidad máxima.

En fases posteriores:

El sensor de luz controlará la iluminación.

El buzzer se activará si se supera la capacidad.

**Avance Actual del Proyecto**

En la etapa actual se ha implementado:

-Sensor de proximidad conectado al Arduino

-Detección de ingreso de personas

-Conteo de personas dentro del estadio

-Envío de datos mediante comunicación serial

-Procesamiento de eventos en Java

Este avance se centra exclusivamente en el control de ingreso de personas.

**Autor(es)**

Andres Felipe Carrasquilla Gutierrez

Diego Alejandro Aguilera Diaz

Yohan Stivens Piñarte Diaz

Jhon Franklin Sandoval Segura

Brayan Steven Acosta Vigoya


