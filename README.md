# CareConnect Mobile App Front-End

## Descripción
CareConnect es un sistema para gestionar visitas domiciliarias del Hospital Universitario San Ignacio. La aplicación móvil, desarrollada en Kotlin para Android, permite a las enfermeras auxiliares consultar pacientes asignados, registrar horas de visitas, gestionar insumos consumidos y actualizar estados de visitas. Se integra con el backend móvil vía API REST, sincronizándose con el portal web administrativo y módulos de geocodificación y optimización de rutas.

## Funcionalidades
- **Autenticación**: Login seguro con email y contraseña.
- **Pacientes Asignados**: Visualización de pacientes por fecha, con detalles como dirección y teléfono.
- **Gestión de Visitas**: Registro de horas de llegada/salida, consulta de procedimientos e insumos, y actualización de estados (e.g., "EN_PROGRESO", "FINALIZADA").
- **Interfaz Intuitiva**: Diseño optimizado para uso en campo por enfermeras.

## Tecnologías
- **Lenguaje**: Kotlin
- **IDE**: Android Studio
- **Bibliotecas Clave**:
    - Retrofit: Para peticiones HTTP
    - Gson: Para parseo JSON
    - Material Design: Para UI
- **Mínimo SDK**: API 21 (Android 5.0)

## Requisitos
- Android Studio (versión Koala o superior)
- JDK 17
- Dispositivo/emulador Android (API 21+)
- Backend móvil de CareConnect activo
- Archivo `local.properties` con:
  ```
  BASE_URL=http://localhost:8000/api
  ```

## Instalación
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/careconnect/mobile-frontend.git
   cd mobile-frontend
   ```

2. Abrir en Android Studio:
    - Selecciona `File > Open` y elige el directorio del proyecto.

3. Configurar `local.properties`:
    - Añade la URL del backend en `local.properties`.

4. Sincronizar proyecto:
    - Haz clic en `Sync Project with Gradle Files`.

5. Ejecutar la app:
    - Conecta un dispositivo o emulador y selecciona `Run > Run 'app'`.

## Uso
- **Inicio de Sesión**: Ingresa credenciales de enfermera para obtener un token JWT.
- **Navegación**:
    - **Pacientes**: Lista de pacientes asignados para la fecha actual.
    - **Visitas**: Registra horas, consulta procedimientos/insumos y actualiza estados.
- **Errores**: Notificaciones para fallos como credenciales inválidas (401) o recursos no encontrados (404).

## Despliegue
- Generar APK:
    - `Build > Build Bundle(s) / APK(s) > Build APK`.
- Firmar APK para publicación:
    - Usa `Build > Generate Signed Bundle / APK`.
- Distribuir vía Google Play o sideload.

## Autoría
- Juan David González
- Lina María Salamanca
- Laura Alexandra Rodríguez
- Axel Nicolás Caro

**Pontificia Universidad Javeriana**  
**Mayo 26, 2025**