[![Build Status](https://travis-ci.com/flowcommerce/api.svg?token=8bzVqzHy6JVEQr9mN9hx&branch=master)](https://travis-ci.com/flowcommerce/lib-localization)

# lib-localization

Library to retrieve localized data

## Instantiating a Localizer

   `val localizer = Localizer(new RedisClientPool("redis_host", 6379))`

## Publishing a new version

	go run release.go

## Publishing a new snapshot for local development

	edit build.sbt and append -SNAPSHOT to version
	sbt +publishLocal

