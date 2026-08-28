# Firma de distribución de CG Gestión

Las APK beta publicadas hasta la versión 12 están firmadas con la clave de
depuración del equipo de desarrollo. Esa firma permite actualizar entre dichas
betas, pero no debe utilizarse como firma de producción.

## Migración recomendada

1. Antes de instalar la primera APK de producción, cada dispositivo debe crear
   un respaldo desde **Respaldos** y copiarlo a una ubicación segura.
2. El responsable crea una clave de lanzamiento privada y la conserva fuera del
   repositorio, junto con sus contraseñas, en un gestor de secretos respaldado.
3. Se crea `keystore.properties` desde la plantilla siguiente (el archivo está
   ignorado por Git). No se guardan contraseñas en este archivo:

   ```properties
   storeFile=release/CGGestion-release.jks
   keyAlias=cggestion
   ```

   Las contraseñas se leen desde las variables de entorno de usuario de Windows
   `CGGESTION_STORE_PASSWORD` y `CGGESTION_KEY_PASSWORD`.

4. Se genera y verifica una APK `release` firmada, incluyendo su certificado
   SHA-256 en el registro de publicación.
5. En cada teléfono se desinstala la beta anterior, se instala la primera APK
   de producción y se restaura el respaldo creado en el paso 1.

Android no acepta una actualización directa cuando cambia el certificado de
firma. La reinstalación única evita que una futura APK firmada correctamente
falle al instalarse sobre la beta.

## Antes de publicar

- Ejecutar `assembleRelease` con `keystore.properties` local y ambas variables
  de entorno configuradas. Gradle rechaza una release sin firma.
- Verificar que la APK tenga el certificado esperado con `apksigner verify --print-certs`.
- Calcular SHA-256 de la APK publicada y actualizar `update.json`.
- Instalar primero en un dispositivo de prueba y comprobar cotizaciones, hojas,
  evidencias, firmas, PDFs y restauración del respaldo.
