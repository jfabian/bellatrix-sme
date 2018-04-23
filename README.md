# Evaluación Bellatrix SME
El aplicativo extrae información de una lista de web's. Puede extraer los hashtags, cuentas de Twitter o nombres propios según se le indique como argumento. Como resultado genera un archivo por cada web como la lista de elementos encontrados.

## Uso

El executable se encuentra en ```build/bellatrix-sme.jar```, acepta los siguientes parámetros:

```
 -i,--input <arg>    Archivo del listado de sitios webs.
 -m,--method <arg>   Tipo de informacion a extraer [hashtag, username, propername].
 -o,--output <arg>   Carpeta donde se guarda el resultado.
```

Existe un ejemplo del archivo de las URL's en la ruta ```data/sample-url.txt```

## Ejemplo

Si queremos extraer todos hos hashtags en la lista de web's (```urls.txt```) ejecutamos el siguiente comando:

```
java -jar bellatrix-sme.jar -i urls.txt -o result-app/ -m hashtag
```

Creará un archivo por cada web en la carpeta ```result-app/``` con el listado de hashtags encontrados.
