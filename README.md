# kv

Does what it says on the tin.

## Usage

In general, if you receive a non-2XX status code, then something went wrong. The response body will be an error message in plain text. The logs of the container may also contain more information about what went wrong.

See [compose.yaml](./compose.yaml) for an example of how to run the container.

### Enumerating entries

```bash
curl -H "Authorization: Bearer abc123" http://localhost:9000
```

```http
HTTP/1.1 200 OK

[
    {
        "key": "example-key",
        "expiresAt": 1775755412479,
        "contentType": "text/plain",
        "lastModified": 1775755412479
    }
]
```

`expiresAt` is a unix timestamp in milliseconds. `-1` means it does not expire.
`lastModified` is a unix timestamp in milliseconds indicating the last modification time.

### Getting an entry

```bash
curl -H "Authorization: Bearer abc123" http://localhost:9000/example-key
```

```http
HTTP/1.1 200 OK
X-Expires-At: 1775755412479
X-Last-Modified: 1775755412479
X-Content-Type: text/plain
Content-Disposition: filename="example-key"

foo=bar
bar=foo
2+2=fish
```

`X-Expires-At` is a unix timestamp in milliseconds. `-1` means it does not expire.
`X-Last-Modified` is a unix timestamp in milliseconds indicating the last modification time.

### Deleting an entry

```bash
curl -X DELETE -H "Authorization: Bearer abc123" http://localhost:9000/example-key
```

```http
HTTP/1.1 200 OK
```

### Setting an entry

```bash
curl -X POST \
    -H "Authorization: Bearer abc123" \
    -H "Content-Type: text/plain" \
    -d "foo=bar\nbar=foo\n2+2=fish" \
    http://localhost:9000/example-key?ttl=3600
```

`ttl` is the time-to-live in seconds. Set to `-1` or omit for no expiration.

```http
HTTP/1.1 201 Created
```

## Implementation notes

- The KV enumerates the entries every 5 minutes to check for expired entries. Calling get() on an expired entry will also trigger an expiration check for that entry, but it is possible for expired entries to exist on disk for up to 5 minutes after they expire.
- Keys must match `^[a-zA-Z0-9%._-]+$`. This is to ensure that keys can be safely used as filenames on disk without needing to be escaped. You can use URL encoding to use characters outside of this set.
- Cache entries are validated and modified using `synchronized` blocks to ensure that concurrent requests do not cause race conditions. This can cause some performance issues under heavy load. In general, the performance should be sufficient for most use cases when modern SSDs are used for the filesystem, but you may want to consider using a different solution if you expect issues with concurrency.
- The KV is not designed to handle shared storage. Do not run multiple instances of the KV pointing to the same storage volume. Doing so may cause data corruption and loss.

Future versions may improve on these limitations, but for now, these are the constraints of the current implementation.
