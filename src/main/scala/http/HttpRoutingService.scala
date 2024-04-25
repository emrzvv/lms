package http

trait HttpRoutingService extends HttpBaseService with BookService {
  this: Repositories with ActorSystemComponent with Serializers with HttpRoute =>
}
