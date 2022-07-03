## there are some dependencies which are not jar, but maybe pom, aar,... make sure they work, too
assert "kscript ${PROJECT_DIR}/test/resources/depends_on_with_type.kts" "getBigDecimal(1L): 1"

# make sure that @file:DependsOn is parsed correctly
assert "kscript ${PROJECT_DIR}/test/resources/depends_on_annot.kts" "kscript with annotations rocks!"

# make sure that @file:DependsOnMaven is parsed correctly
assert "kscript ${PROJECT_DIR}/test/resources/depends_on_maven_annot.kts" "kscript with annotations rocks!"

# make sure that dynamic versions are matched properly
assert "kscript ${PROJECT_DIR}/test/resources/depends_on_dynamic.kts" "dynamic kscript rocks!"

# make sure that @file:MavenRepository is parsed correctly
assert "kscript ${PROJECT_DIR}/test/resources/custom_mvn_repo_annot.kts" "kscript with annotations rocks!"
assert_stderr "kscript ${PROJECT_DIR}/test/resources/illegal_depends_on_arg.kts" '[kscript] [ERROR] Artifact locators must be provided as separate annotation arguments and not as comma-separated list: [com.squareup.moshi:moshi:1.5.0,com.squareup.moshi:moshi-adapters:1.5.0]'

# make sure that @file:MavenRepository is parsed correctly
assert "kscript ${PROJECT_DIR}/test/resources/script_with_compile_flags.kts" "hoo_ray"

## Ensure dependencies are solved correctly #345
rm -rf ~/.m2/repository/com/beust
assert "kscript ${PROJECT_DIR}/test/resources/depends_on_klaxon.kts" "Successfully resolved klaxon"
