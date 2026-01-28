# Compiling from source

## Requirements

To compile WaterdogPE you need JDK 17.

### Recommended libraries and versions

- Project uses awesome Protocol library provided by CloudBurst. Their library should be compatible with WDPE.
- **For advanced users:** If you require using custom build of raknet library, you can change it inside of `pom.xml`
  file using `raklib.version` property. Make sure your version is compatible with WDPE and Protocol library.

## How to complete this guide

- To get latest WDPE source we recommend cloning our GitHub repository
  using `git clone https://github.com/WaterdogPE/WaterdogPE.git`.
- cd into `WaterdogPE` folder.
- Compile sources using Maven. You can use Maven Wrapper `mvnw clean install`.
- Once Maven finishes the build you can find your executable `Waterdog.jar` in `target` folder.
