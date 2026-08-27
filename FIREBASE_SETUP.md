# Configuración de Firebase para CG Gestión

La aplicación usa Firebase exclusivamente para iniciar sesión y consultar el rol. Los datos operativos continúan en Room.

## 1. Proyecto y aplicación Android

1. Crea un proyecto en Firebase Console.
2. Registra una aplicación Android con el package `com.example.cggestion`.
3. Descarga `google-services.json` y colócalo en `app/google-services.json`.
4. En Authentication, habilita **Correo electrónico/Contraseña**.
5. Crea Cloud Firestore en modo producción.
6. Copia el contenido de `firebase/firestore.rules` en Firestore > Reglas y publícalo.

## 2. Crear el superusuario

1. En Authentication > Users crea la cuenta con el correo real del propietario.
2. Copia el UID generado.
3. En Firestore crea la colección `usuarios` y un documento cuyo ID sea exactamente ese UID.
4. Agrega estos campos:

| Campo | Tipo | Valor |
| --- | --- | --- |
| `email` | string | Correo real de la cuenta |
| `nombre` | string | Nombre visible |
| `rol` | string | `SUPERUSUARIO` |
| `activo` | boolean | `true` |
| `fechaCreacion` | timestamp | Fecha actual |

Solo debe existir un perfil con rol `SUPERUSUARIO`.

## 3. Crear técnicos

Repite el proceso anterior para cada técnico y usa `TECNICO` en el campo `rol`. Para bloquear una cuenta, deshabilítala en Authentication y cambia `activo` a `false` en su perfil.

La contraseña inicial se define al crear la cuenta. El técnico puede cambiarla usando **Olvidé mi contraseña** en la aplicación. Configura la plantilla y el idioma del correo en Authentication > Templates.

## 4. Consideraciones

- El primer inicio de sesión requiere internet.
- Después del primer acceso, el perfil queda disponible sin conexión en el almacenamiento privado de la aplicación.
- Un bloqueo remoto se aplica cuando el dispositivo vuelve a conectarse.
- No agregues usuarios desde la tabla Room heredada `usuarios`; esa tabla permanece únicamente por compatibilidad con instalaciones anteriores.
