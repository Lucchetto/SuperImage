import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidBuildFlavours: Plugin<Project> {

    override fun apply(target: Project) = with(target.extensions.findByName("android") as CommonExtension<*, *, *, *>) {

        flavorDimensions.add("version")

        productFlavors {
            create("free") {
                dimension = "version"
            }
            create("playstore") {
                dimension = "version"
            }
        }
    }
}
