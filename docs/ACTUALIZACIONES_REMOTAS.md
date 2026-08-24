# Publicar una actualización remota

1. Incrementa `versionCode` y `versionName` en `app/build.gradle.kts`.
2. Genera una APK release firmada con la misma clave usada en las versiones anteriores.
3. Calcula el hash: `Get-FileHash ruta\\CGGestion.apk -Algorithm SHA256`.
4. Crea una GitHub Release estable con tag `v<versionCode>` y adjunta la APK.
5. Adjunta también un archivo `update.json` con este formato:

```json
{
  "versionCode": 8,
  "versionName": "1.7-beta",
  "apkUrl": "https://github.com/OthoC/CGGestion/releases/download/v8/CGGestion-8.apk",
  "sha256": "HASH_SHA256_EN_MINUSCULAS",
  "notes": "Resumen de cambios.",
  "date": "2026-08-24"
}
```

La aplicación consulta `releases/latest/download/update.json`. El `versionCode` debe ser mayor que el instalado y la APK debe conservar la misma firma release.

## Firma local

No subas claves al repositorio. Crea `keystore.properties` en la raíz, ignorado por Git:

```properties
storeFile=keystore/cggestion-release.jks
storePassword=CONTRASENA_SEGURA
keyAlias=cggestion
keyPassword=CONTRASENA_SEGURA
```
