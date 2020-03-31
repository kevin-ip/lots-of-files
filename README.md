# lots-of-files
Create lots of random files

## build
require java 8

```
./gradlew build
```

## Run

Once build is completed, see help menu
`cd build/libs; java -jar lots-of-files-all.jar -h`

### Create files
#### Help
`java -jar lots-of-files-all.jar create -h`

#### create files
`java -jar lots-of-files-all.jar create --number-of-files=15 $PATH`

e.g.
```
java -jar lots-of-files-all.jar create --number-of-files=15 /tmp
```
