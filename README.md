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



