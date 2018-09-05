package kscript.app

fun buildPom(depIds: List<String>, customRepos: List<MavenRepo>): String {
    val depTags = depIds.map {
        val regex = Regex("^([^:]*):([^:]*):([^:@]*)(:(.*))?(@(.*))?\$")
        val matchResult = regex.find(it)

        if (matchResult == null) {
            System.err.println("[ERROR] Invalid dependency locator: '${it}'.  Expected format is groupId:artifactId:version[:classifier][@type]")
            quit(1)
        }

        """
    <dependency>
            <groupId>${matchResult.groupValues[1]}</groupId>
            <artifactId>${matchResult.groupValues[2]}</artifactId>
            <version>${matchResult.groupValues[3]}</version>
            ${matchResult.groups[5]?.let { "<classifier>" + it.value + "</classifier>"} ?: ""}
            ${matchResult.groups[7]?.let { "<type>" + it.value + "</type>"} ?: ""}
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
