USE sigoc_db;
INSERT INTO rol(nombre_rol)
SELECT 'ADMIN' WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre_rol='ADMIN');
INSERT INTO rol(nombre_rol)
SELECT 'INGENIERO' WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre_rol='INGENIERO');
INSERT INTO rol(nombre_rol)
SELECT 'ALMACENERO' WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre_rol='ALMACENERO');
INSERT INTO rol(nombre_rol)
SELECT 'CONTADOR' WHERE NOT EXISTS (SELECT 1 FROM rol WHERE nombre_rol='CONTADOR');
INSERT INTO usuario(nombre_completo,usuario_login,contrasena_hash,id_rol,estado)
SELECT 'Administrador SIGOC','admin','PBKDF2$120000$U0lHT0MtREVNT1NUUkFDSU9OLVNBTFQ=$rTzG6iZdG+6OFOOfY11QKZauNQ6Az1Yie4YbgDMcUT8=',id_rol,1
FROM rol WHERE nombre_rol='ADMIN' AND NOT EXISTS (SELECT 1 FROM usuario WHERE usuario_login='admin');

-- Credenciales para grupos de clase (contraseña: SIGOC2026)
INSERT INTO usuario(nombre_completo,usuario_login,contrasena_hash,id_rol,estado)
SELECT 'Grupo 1','g1','PBKDF2$120000$HHZ4sMpeU/nvfEprgG4/WA==$ftLgi++PX4wXfI5tXPc8DGTB/hwNFJM5o4jqGJC0d64=',id_rol,1
FROM rol WHERE nombre_rol='ADMIN' AND NOT EXISTS (SELECT 1 FROM usuario WHERE usuario_login='g1');
INSERT INTO usuario(nombre_completo,usuario_login,contrasena_hash,id_rol,estado)
SELECT 'Grupo 2','g2','PBKDF2$120000$uHa0CA0PW4oevsAamKHi1A==$JsOyVN6AGTL8Z+akjr5gFs9Dvd/WeXWIGG9JJ1TEoMo=',id_rol,1
FROM rol WHERE nombre_rol='INGENIERO' AND NOT EXISTS (SELECT 1 FROM usuario WHERE usuario_login='g2');
INSERT INTO usuario(nombre_completo,usuario_login,contrasena_hash,id_rol,estado)
SELECT 'Grupo 3','g3','PBKDF2$120000$8sHNNkFKehRJD/uKVUdUPA==$N8ZSwKWSp690dOrxrMlHCPEDBzjAwku0yF2MtM8pWio=',id_rol,1
FROM rol WHERE nombre_rol='ALMACENERO' AND NOT EXISTS (SELECT 1 FROM usuario WHERE usuario_login='g3');
INSERT INTO usuario(nombre_completo,usuario_login,contrasena_hash,id_rol,estado)
SELECT 'Grupo 4','g4','PBKDF2$120000$LRmuqgGAcX6jD/KUk4mLgw==$wbYWxykRtBnb8ljQChAc7iLEkoQr/R8gX5TBiM9yFHg=',id_rol,1
FROM rol WHERE nombre_rol='CONTADOR' AND NOT EXISTS (SELECT 1 FROM usuario WHERE usuario_login='g4');

INSERT INTO material(nombre,unidad_medida,stock_actual,stock_minimo,costo_unitario_ref)
SELECT 'Cemento Portland Tipo I','bolsa',0,20,30.00 WHERE NOT EXISTS (SELECT 1 FROM material WHERE nombre='Cemento Portland Tipo I');
INSERT INTO material(nombre,unidad_medida,stock_actual,stock_minimo,costo_unitario_ref)
SELECT 'Arena gruesa','m3',0,5,55.00 WHERE NOT EXISTS (SELECT 1 FROM material WHERE nombre='Arena gruesa');
INSERT INTO material(nombre,unidad_medida,stock_actual,stock_minimo,costo_unitario_ref)
SELECT 'Piedra chancada','m3',0,5,75.00 WHERE NOT EXISTS (SELECT 1 FROM material WHERE nombre='Piedra chancada');
