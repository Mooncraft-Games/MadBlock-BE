# MadBlock Bedrock Edition Repository

### Cloning

```
git clone https://github.com/Mooncraft-Games/MadBlock-BE.git
git submodule update --init
```

### Building (after cloning)

**All:** `./gradlew packageProduction`

**Tools:** `./gradlew :Tool:clean :Tool:assemble copyJars`

**Components:** `./gradlew :Component:clean :Component:assemble copyJars`

**Games:** `./gradlew :Game:clean :Game:assemble copyJars`

