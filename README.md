[![Build Status](https://travis-ci.com/flowcommerce/api.svg?token=8bzVqzHy6JVEQr9mN9hx&branch=master)](https://travis-ci.com/flowcommerce/lib-localization)

# lib-localization

Library to retrieve localized data from a redis store.

## Instantiating a Localizer

```
import com.twitter.finagle.redis
import io.flow.localization.Localizer

val redisClient = redis.Client("localhost:6379")
val localizer = Localizer(redisClient)
```


## Publishing a new version

	go run release.go

## Publishing a new snapshot for local development

	edit build.sbt and append -SNAPSHOT to version
	sbt +publishLocal

