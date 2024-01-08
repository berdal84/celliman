
# Celliman is a Fiji plugin to analyse cell images written in Java

The goal of Celliman is to:
- count from cytolosic staining
- count from nuclaer staining
- compute co-localization
- save a report (count, and ratio in percent)

to be continued...

Berdal84.

## How to use?

- Browse and download the [latest release](https://github.com/berdal84/celliman/releases/latest).
- Unzip file content into `<Fiji.app>/plugins/`
- Close and re-run Fiji.
- You should now see Celliman at the bottom of the Plugin menu (it is also searchable by name).
  
## How to build?

Note: command line instructions are **not** Windows compatible.

Prerequisites:
- Fiji must be installed
- Open JDK must be installed
- Maven must be installed
- Git must be installed

Clone (or download) this repository and run the build script.

```
clone https://github.com/berdal84/celliman
cd celliman
mvn install
```

The built command should be present in the `./target/` folder.

Optional: You might want to build and copy the package to your fiji app. In order to do that, you can edit
build.sh with your own Fiji path and run `./build.sh` instead of `mvn install`

