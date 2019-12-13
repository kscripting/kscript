## Release Checklist

# 1. Increment version in `Kscript.kt`
# 2. Make sure that support api version is up to date and available from jcenter
# 3. Push and wait for travis CI results

#export KSCRIPT_HOME="/Users/brandl/projects/kotlin/kscript";
export KSCRIPT_HOME="/mnt/hgfs/sharedDB/db_projects/kscript";
export PATH=${KSCRIPT_HOME}:${PATH}
export PATH=~/go/bin/:$PATH

KSCRIPT_ARCHIVE=~/archive/kscript_versions/


kscript_version=$(grep 'val KSCRIPT_VERSION' ${KSCRIPT_HOME}/src/main/kotlin/kscript/app/Kscript.kt | cut -f2 -d'=' | tr -d ' "')
echo "new version is $kscript_version" 
## see https://github.com/aktau/github-release


########################################################################
### Build the binary release


## create and upload deployment file for sdkman

cd $KSCRIPT_HOME

./gradlew clean assemble

## compile binary distribution (including jar and wrapper-script)
mkdir -p $KSCRIPT_ARCHIVE/kscript-${kscript_version}/bin
cp ${KSCRIPT_HOME}/build/libs/kscript ${KSCRIPT_ARCHIVE}/kscript-${kscript_version}/bin
cp ${KSCRIPT_HOME}/build/libs/kscript.jar ${KSCRIPT_ARCHIVE}/kscript-${kscript_version}/bin/kscript.jar

cd ${KSCRIPT_ARCHIVE}
rm -f ${KSCRIPT_ARCHIVE}/kscript-${kscript_version}.zip

# on linux
zip -r ${KSCRIPT_ARCHIVE}/kscript-${kscript_version}.zip kscript-${kscript_version}
# on windows
#docker run -it --rm -e kscript_version="${kscript_version}" -v  c:/Users/brandl/archive/kscript_versions:/data alpine
#apk add zip && cd /data &&  zip -r kscript-${kscript_version}.zip kscript-${kscript_version}


open ${KSCRIPT_ARCHIVE}


## Ensure correct targetCompatibility

#Java SE 6.0 = 50 (0x32 hex) (from https://en.wikipedia.org/wiki/Java_class_file)
#Default is 50, see https://kotlinlang.org/docs/reference/using-gradle.html#attributes-common-for-jvm-and-js
## to insepct do
#./gradlew clean assemble
#cd ${KSCRIPT_BUILD_LIBS}
#rm -rf kscript_target_test
#cp -f kscript.jar kscript_target_test.zip
#unzip kscript_target_test.zip -d kscript_target_test
#javap -verbose kscript_target_test/org/docopt/Python.class | head
#export JAVA_HOME=`/usr/libexec/java_home -v 1.8.0_151`


########################################################################
### Do the github release

## create tag on github 
#github-release --help

source ~/archive/gh_token.sh
export GITHUB_TOKEN=${GH_TOKEN}
#echo $GITHUB_TOKEN

# make your tag and upload
cd ${KSCRIPT_HOME}

git config  user.email "holgerbrandl@users.noreply.github.com"


#git tag v${kscript_version} && git push --tags
(git diff --exit-code && git tag v${kscript_version})  || echo "could not tag current branch"
git push --tags

# check the current tags and existing releases of the repo
github-release info -u holgerbrandl -r kscript

# create a formal release
github-release release \
    --user holgerbrandl \
    --repo kscript \
    --tag "v${kscript_version}" \
    --name "v${kscript_version}" \
    --description "See [NEWS.md](https://github.com/holgerbrandl/kscript/blob/master/NEWS.md) for changes." 
#    \
#    --pre-release


## upload sdk-man binary set
github-release upload \
    --user holgerbrandl \
    --repo kscript \
    --tag "v${kscript_version}" \
    --name "kscript-${kscript_version}-bin.zip" \
    --file ${KSCRIPT_ARCHIVE}/kscript-${kscript_version}.zip


########################################################################
### Update release branch

#http://stackoverflow.com/questions/13969050/how-to-create-a-new-empty-branch-for-a-new-project

cd $KSCRIPT_HOME && rm -rf kscript_releases_*

git clone https://github.com/holgerbrandl/kscript kscript_releases_${kscript_version}
cd kscript_releases_${kscript_version}

git config  user.email "holgerbrandl@users.noreply.github.com"

#git checkout --orphan releases
#git reset --hard
#git rm --cached -r .

git checkout releases
#cp ~/projects/kotlin/kscript/resdeps.kts ~/projects/kotlin/kscript/kscript .
#cp ~/projects/kotlin/kscript/kscript .
## create file with current release version
echo "
KSCRIPT_VERSION=${kscript_version}
" > kscript

git add -A 
git status
git commit -m "v${kscript_version} release"

git push origin releases

## test the updated version pointer
curl https://raw.githubusercontent.com/holgerbrandl/kscript/releases/kscript

########################################################################
### release on sdkman

## see docs http://sdkman.io/vendors.html
# Summary: sequence of calls is as above, first **releasing** , then making it **default** , and finally **announce** it to the world.

## from .bash_profile
source ~/archive/kscript_sdkman_json.sh
echo ${SDKMAN_CONSUMER_KEY} ${SDKMAN_CONSUMER_TOKEN} ${kscript_version}
#echo ${SDKMAN_CONSUMER_KEY} | cut -c-5
#echo ${SDKMAN_CONSUMER_TOKEN} | cut -c-5


## test the binary download
#cd ~/Desktop
#wget https://github.com/holgerbrandl/kscript/releases/download/v${kscript_version}/kscript-${kscript_version}-bin.zip
#unzip kscript-${kscript_version}-bin.zip

#kscript_version=1.5.1

curl -X POST \
    -H "Consumer-Key: ${SDKMAN_CONSUMER_KEY}" \
    -H "Consumer-Token: ${SDKMAN_CONSUMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d '{"candidate": "kscript", "version": "'${kscript_version}'", "url": "https://github.com/holgerbrandl/kscript/releases/download/v'${kscript_version}'/kscript-'${kscript_version}'-bin.zip"}' \
    https://vendors.sdkman.io/release


## Set existing Version as Default for Candidate

curl -X PUT \
    -H "Consumer-Key: ${SDKMAN_CONSUMER_KEY}" \
    -H "Consumer-Token: ${SDKMAN_CONSUMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d '{"candidate": "kscript", "version": "'${kscript_version}'"}' \
    https://vendors.sdkman.io/default

## Broadcast a Structured Message
curl -X POST \
    -H "Consumer-Key: ${SDKMAN_CONSUMER_KEY}" \
    -H "Consumer-Token: ${SDKMAN_CONSUMER_TOKEN}" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d '{"candidate": "kscript", "version": "'${kscript_version}'", "hashtag": "kscript"}' \
    https://vendors.sdkman.io/announce/struct


########################################################################
### Update the homebrew descriptor (see https://github.com/holgerbrandl/kscript/issues/50)

cd $KSCRIPT_HOME && rm -rf homebrew-tap
git clone https://github.com/holgerbrandl/homebrew-tap.git
cd homebrew-tap

git config  user.email "holgerbrandl@users.noreply.github.com"

archiveMd5=$(shasum -a 256 ${KSCRIPT_ARCHIVE}/kscript-${kscript_version}.zip | cut -f1 -d ' ')

cat - <<EOF > kscript.rb
class Kscript < Formula
  desc "kscript"
  homepage "https://github.com/holgerbrandl/kscript"
  url "https://github.com/holgerbrandl/kscript/releases/download/v${kscript_version}/kscript-${kscript_version}-bin.zip"
  sha256 "${archiveMd5}"

  depends_on "kotlin"

  def install
    libexec.install Dir["*"]
    inreplace "#{libexec}/bin/kscript", /^jarPath=.*/, "jarPath=#{libexec}/bin/kscript.jar"
    bin.install_symlink "#{libexec}/bin/kscript"
  end
end
EOF

git add kscript.rb
git commit -m "v${kscript_version} release"
git push #origin releases


## to test use `brew install holgerbrandl/tap/kscript`



########################################################################
### Update the archlinux package (see https://aur.archlinux.org/packages/kscript/ and https://github.com/holgerbrandl/kscript/pull/216/)

cd $KSCRIPT_HOME && rm -rf archlinux

# See https://wiki.archlinux.org/index.php/AUR_submission_guidelines#Authentication for credentials setup
git clone ssh://aur@aur.archlinux.org/kscript.git archlinux

cd archlinux


#update the PKGBUILD file/pkgver variable
cat - <<EOF > PKGBUILD
# Maintainer: Holger Brandl https://github.com/holgerbrandl/kscript/

pkgname=kscript
pkgver=${kscript_version}
pkgrel=1
pkgdesc='Enhanced scripting support for Kotlin on *nix-based systems'
arch=('any')
url='https://github.com/holgerbrandl/kscript'
license=('MIT')
depends=('kotlin')
source=("\${pkgname}-\${pkgver}.bin.zip::https://github.com/holgerbrandl/\${pkgname}/releases/download/v\${pkgver}/\${pkgname}-\${pkgver}-bin.zip")
sha256sums=('${archiveMd5}')

package() {
    cd "\${srcdir}/\${pkgname}-\${pkgver}/bin"

    install -Dm 755 kscript "\${pkgdir}/usr/bin/kscript"
    install -Dm 644 kscript.jar "\${pkgdir}/usr/bin/kscript.jar"
}

EOF

#update the PKGBUILD file/pkgver variable
cat - <<EOF > .SRCINFO
pkgbase = kscript
	pkgdesc = Enhanced scripting support for Kotlin on *nix-based systems
	pkgver = ${kscript_version}
	pkgrel = 1
	url = https://github.com/holgerbrandl/kscript
	arch = any
	license = MIT
	depends = kotlin
	source = kscript-${kscript_version}.bin.zip::https://github.com/holgerbrandl/kscript/releases/download/v${kscript_version}/kscript-${kscript_version}-bin.zip
	sha256sums = ${archiveMd5}

pkgname = kscript

EOF

#https://wiki.manjaro.org/index.php?title=Makepkg
#updpkgsums
#makepkg --printsrcinfo > .SRCINFO
git add PKGBUILD .SRCINFO
git commit -m "updated to ${kscript_version}"
git push