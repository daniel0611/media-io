import de.dani09.moviedownloader.WebFrontendMode
import javax.servlet.ServletContext
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new WebFrontendMode, "/*")
  }
}
