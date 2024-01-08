echo "Installing ..."
mvn install || (echo "Unable to install" && exit 1)
echo "Copying package ..."
cp ./target/Celliman-0.1.0-SNAPSHOT.jar ~/Fiji.app/plugins/ || (echo "Unable to copy" && exit 1)
echo "Copying package ..."