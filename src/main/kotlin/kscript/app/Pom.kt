package kscript.app

fun buildPom(depIds: List<String>, customRepos: List<MavenRepo>): String {
    val depTags = depIds.map {
        val splitDepId = it.split(":")

        if (!listOf(3, 4).contains(splitDepId.size)) {
            System.err.println("[ERROR] Invalid dependency locator: '${it}'.  Expected format is groupId:artifactId:version[:classifier]")
            quit(1)
        }

        """
    <dependency>
            <groupId>${splitDepId[0]}</groupId>
            <artifactId>${splitDepId[1]}</artifactId>
            <version>${splitDepId[2]}</version>
            ${if (splitDepId.size == 4) "<classifier>" + splitDepId[3] + "<classifier>" else ""}
    </dependency>
    """
    }

    // see https://github.com/holgerbrandl/kscript/issues/22
    val repoTags = customRepos.map {
        """
    <repository>
            <id>${it.id}</id>
            <url>${it.url}</url>
    </repository>
    """

    }

    return """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>kscript</groupId>
    <artifactId>maven_template</artifactId>
    <version>1.0</version>

     <repositories>
        <repository>
            <id>jcenter</id>
            <url>http://jcenter.bintray.com/</url>
        </repository>
        ${repoTags.joinToString("\n")}
    </repositories>

    <dependencies>
    ${depTags.joinToString("\n")}
    </dependencies>
</project>
"""
}
