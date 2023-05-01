<h1 align="center">Buildable</h1></br>


<p align="center">
  <a href="https://github.com/behzod1996/izoh"><img  alt="Buildable Cover" src="https://github.com/getspherelabs/buildable/blob/main/docs/images/cover-buildable.png?raw=true"/></a> <br>
</p>

<a href="https://github.com/getspherelabs/buildable/actions/workflows/android.yml"><img alt="Build Status" 
  src="https://github.com/getspherelabs/buildable/actions/workflows/android.yml/badge.svg"/></a>
  
## Why Buildable?

`@Buildable` is a tool designed for Kotlin that facilitates the generation of code for mappers, factories, and state classes. By automating the creation of these components, Buildable can save developers a significant amount of time and effort,

## Build Types

`@Buildable` provides support for three unique build types.

### Factory

Generates factory methods for creating instances of classes with complex constructors or dependencies. 

<p align="center">
<a href="https://github.com/behzod1996/izoh"><img  alt="Buildable Cover" src="https://github.com/getspherelabs/buildable/blob/main/docs/images/BuildableFactory%20-%20Code%20Usage.png?raw=true" width="760" /></a> <br>
</p>

### Mapper

Generates mapping functions that facilitate the conversion of data between different data classes. This is particularly useful in situations where you need to map data from API responses to your application's domain models, or between different layers of your application.

<p align="center">

<a href="https://github.com/behzod1996/izoh"><img  alt="Buildable Cover" src="https://github.com/getspherelabs/buildable/blob/main/docs/images/BuildableMapper%20-%20Code%20Usage%202.png?raw=true" width="760" /></a> <br>
</p>

### State (Working in Progress)

## Configuring Gradle

In order to use [KSP (Kotlin Symbol Processing)](https://kotlinlang.org/docs/ksp-quickstart.html) and the [Buildable](https://github.com/getspherelabs/buildable) library into your project, follow the steps below.

### Enable KSP in your module

To enable KSP in your module, add the KSP plugin as shown below to your module's `build.gradle` file:

<details open>
  <summary>Kotlin (KTS)</summary>
  
```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
}
```
</details>

<details>
  <summary>Groovy</summary>

```kotlin
plugins {
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
}
```
</details>

> **Note**: Make sure your current Kotlin version and [KSP version](https://github.com/google/ksp/releases) is the same.

## Add the Buildable library to your module

Add the dependency below into your **module**'s `build.gradle` file:

```gradle
dependencies {
    implementation("io.github.behzodhalil:buildable-mapper-core:1.1.0")
    ksp("io.github.behzodhalil:buildable-mapper:1.1.0)
}
```
### Add source path (KSP)

To access generated codes from KSP, you need to set up the source path like the below into your **module**'s `build.gradle` file:

<details open>
  <summary>Android Kotlin (KTS)</summary>

```kotlin
kotlin {
  sourceSets.configureEach {
    kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
  }
}
```
</details>

<details>
  <summary>Android Groovy</summary>

```gradle
android {
    applicationVariants.all { variant ->
        kotlin.sourceSets {
            def name = variant.name
            getByName(name) {
                kotlin.srcDir("build/generated/ksp/$name/kotlin")
            }
        }
    }
}
```
</details>

<details>
  <summary>Pure Kotlin (KTS)</summary>

```gradle
kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
```
</details>

<details>
  <summary>Pure Kotlin Groovy</summary>

```gradle
kotlin {
    sourceSets {
        main.kotlin.srcDirs += 'build/generated/ksp/main/kotlin'
        test.kotlin.srcDirs += 'build/generated/ksp/test/kotlin'
    }
}
```
</details>

## Usage and Examples

### BuildableFactory

`@BuildableFactory` annotation aims to simplify code generation associated with the Factory Method pattern. This simplifies the management of object creation, ultimately enhancing maintainability and code readability.
- `@BuildableComponent` annotation is utilized to specify the type for creating a factory pattern implementation.

```kotlin
@BuildableFactory
interface Car {
  fun drive()
}

@BuildableComponent
class Nexia : Car {
  override fun drive() {
    println("Nexia is driving...")
  }
}

@BuildableComponent
class Matiz : Car {
  override fun drive() {
    println("Matiz is driving...")
  }
```

The example codes generate `CarFactory` and `CarTypes` for easier object management.

**CarFactory (generated)**:
```kotlin
public enum class CarType {
  NEXIA,
  MATIZ,
}

public fun CarFactory(key: CarType): Car = when (key) {
  CarType.NEXIA -> Nexia()
  CarType.MATIZ -> Matiz()
}
```

### BuildableMapper

 `@BuildableMapper` annotation is designed to streamline code generation for mapping one class to another, making object creation management more efficient and improving overall maintainability and readability within the codebase.

```kotlin
@BuildableMapper(
  from = [NotificationEntity::class],
  to = [NotificationEntity::class]
)
data class NotificationDto(
  val name: String
)

data class NotificationEntity(
  val name: String
)
```

The example codes generate `NotificationDtoBuildableMapperExtensions.kt` for easier object management.

```kotlin
fun NotificationEntity.toNotificationDto() = NotificationDto(
	name = this.name,
)

fun NotificationDto.toNotificationEntity() = NotificationEntity(
	name = this.name,
)
```
