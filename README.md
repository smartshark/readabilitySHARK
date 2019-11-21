# readabilitySHARK
[![Build Status](https://travis-ci.com/smartshark/readabilitySHARK.svg?branch=master)](https://travis-ci.com/smartshark/readabilitySHARK)

This plugin executes readability models to evaluate the readability of source code.

# Readability Models

- Buse, Raymond P. L. and Weimer, Westley R. (2010) "Learning a Metric for Code Readability" IEEE Trans. Softw. Eng., DOI: [10.1109/TSE.2009.70](http://dx.doi.org/10.1109/TSE.2009.70) 
- Scalabrino, Simone and Linares-Vásquez, Mario and Oliveto, Rocco and Poshyvanyk, Denys (2019) "A comprehensive model for code readability" Journal of Software: Evolution and Process, DOI: [10.1002/smr.1958](https://onlinelibrary.wiley.com/doi/abs/10.1002/smr.1958)

# Installation

After cloning the repository the additional files need to be fetched as described in Requirements.
If that is complete the project can be built with gradle,

## Requirements

```bash
cd libs

# fetch Buse and Weimar library
wget -O readability.jar http://www.arrestedcomputing.com/readability/readability.jar?attredirects=0

# fetch Scalabrino et al. library and trained classifier
wget https://dibt.unimol.it/report/readability/files/readability.zip
unzip readability.zip

# classifier needs to be present in the main folder for tests
cp readability.classifier ../
```

## Build SmartSHARK plugin

The project is not built at this step. It will be built later on the ServerSHARK in the install step.

```bash
cd plugin_packaging
./build_plugin.sh
```

## Build

```bash
./gradlew clean assemble
```

## Run tests

```bash
./gradlew clean test
```

## Execute

```bash
java -jar ./build/libs/readabilitySHARK.jar -DB database_name -H database_host -ll INFO -P database_password -U database_user -i path_to_cloned_repository -r full_revision_hash -u project_repository_url -p database_port -a authentication_database --project_name project_name
```

## Example

```bash
java -jar ./build/libs/readabilitySHARK.jar -DB smartshark -H 127.0.0.1 -ll INFO -P balla -U smartshark -i /srv/repos/safe/ -r 57dac9618dc200cc6fb94be1dc4e0c6e9893aded -u https://github.com/openintents/safe.git -p 27018 -a smartshark --project_name oisafe
```
