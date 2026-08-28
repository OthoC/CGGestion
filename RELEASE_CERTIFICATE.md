# Certificado de distribución de CG Gestión

La primera APK de producción de CG Gestión usa un certificado distinto de las
APK beta publicadas hasta v12. El valor siguiente identifica el certificado
público; no permite firmar aplicaciones ni sustituye al keystore privado.

| Algoritmo | SHA-256 del certificado |
| --- | --- |
| RSA 4096 / SHA256withRSA | `1AFBD81367E1BC62269638B5A97E91786FA444C34C89A7B75CDD65BBA6818064` |

Antes de publicar una APK, se debe verificar que `apksigner verify --print-certs`
muestre este mismo SHA-256. Si se pierde el keystore privado, no se debe crear
otro certificado para una actualización existente: será necesario distribuir una
aplicación nueva y migrar los datos mediante respaldo.
