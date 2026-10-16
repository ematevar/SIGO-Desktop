# Respaldo de MySQL en la nube

Verifica primero si el proveedor tiene copias automáticas habilitadas y cuál es su política de retención. Antes de cada entrega genera un respaldo manual desde una terminal:

```powershell
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\sigoc-backups"
mysqldump -h HOST -P 3306 -u USUARIO -p --single-transaction sigoc_db > "$env:USERPROFILE\sigoc-backups\sigoc_db_$(Get-Date -Format yyyyMMdd_HHmmss).sql"
```

Conserva los dumps en una ubicación fuera del repositorio y no incluyas contraseñas en el comando ni en el historial del shell. Restringe el acceso a la carpeta y verifica periódicamente que un dump pueda restaurarse en una base de pruebas.
