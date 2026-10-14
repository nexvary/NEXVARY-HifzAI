"""Loopback-only JSON transport for private integration testing, NOT public hosting."""
import argparse
import getpass
import json
from pathlib import Path
from http.server import BaseHTTPRequestHandler, HTTPServer
from .service import Academy, AcademyError

MAX_BODY = 16_384


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate JSON key')
        result[key] = value
    return result


def make_server(academy, port=0):
    class Handler(BaseHTTPRequestHandler):
        # HTTPServer is single-threaded to bound concurrent memory-hard login work.
        def setup(self):
            super().setup()
            self.connection.settimeout(5)

        def log_message(self, format, *args):
            pass  # No request body, credentials or bearer token in logs.

        def local_client(self):
            allowed = {f'127.0.0.1:{self.server.server_port}', f'localhost:{self.server.server_port}'}
            host = self.headers.get('Host')
            origins = {None, 'http://'+str(host)}
            return (len(self.headers.get_all('Host', [])) == 1 and host in allowed and
                    len(self.headers.get_all('Origin', [])) <= 1 and self.headers.get('Origin') in origins)

        def send_payload(self, status, encoded, content_type):
            self.send_response(status)
            self.send_header('Content-Type', content_type)
            self.send_header('Content-Security-Policy', "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; img-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")
            self.send_header('Referrer-Policy', 'no-referrer')
            self.send_header('X-Frame-Options', 'DENY')
            self.send_header('Content-Length', str(len(encoded)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.send_header('Connection', 'close')
            self.end_headers()
            self.wfile.write(encoded)
            self.close_connection = True

        def send_json(self, status, body):
            self.send_payload(status, json.dumps(body, ensure_ascii=False).encode('utf-8'),
                              'application/json; charset=utf-8')

        def do_GET(self):
            if not self.local_client():
                self.send_json(403, {'error': 'Local integration client required'})
                return
            assets = {'/': ('console.html', 'text/html; charset=utf-8'),
                      '/console.js': ('console.js', 'text/javascript; charset=utf-8'),
                      '/console.css': ('console.css', 'text/css; charset=utf-8')}
            if self.path not in assets:
                self.send_json(404, {'error': 'Resource unavailable'})
                return
            filename, content_type = assets[self.path]
            self.send_payload(200, (Path(__file__).parent / filename).read_bytes(), content_type)

        def do_POST(self):
            try:
                # Exact Host + same-origin-only requests; no cross-site browser access.
                if not self.local_client():
                    raise AcademyError(403, 'Local integration client required')
                if self.path not in ('/v1/login', '/v1/operation'):
                    raise AcademyError(404, 'Operation unavailable')
                if self.headers.get('Transfer-Encoding') or len(self.headers.get_all('Content-Length', [])) != 1:
                    raise AcademyError(400, 'Invalid request framing')
                if self.headers.get('Content-Type') != 'application/json':
                    raise AcademyError(415, 'JSON required')
                length = int(self.headers.get('Content-Length', '0'))
                if not 0 < length <= MAX_BODY:
                    raise AcademyError(413, 'Request size rejected')
                body = self.rfile.read(length)
                if len(body) != length:
                    raise AcademyError(400, 'Incomplete request')
                data = json.loads(body.decode('utf-8'), object_pairs_hook=unique_object)
                if not isinstance(data, dict):
                    raise AcademyError(400, 'JSON object required')
                if self.path == '/v1/login':
                    result = academy.login(data.get('login'), data.get('password'))
                else:
                    auth = self.headers.get('Authorization', '')
                    if not auth.startswith('Bearer '):
                        raise AcademyError(401, 'Authentication required')
                    result = academy.call(auth[7:], data.get('action'), data.get('data', {}))
                self.send_json(200, result)
            except AcademyError as error:
                self.send_json(error.status, {'error': error.message})
            except (ValueError, UnicodeError, RecursionError, TypeError):
                self.send_json(400, {'error': 'Invalid request'})
            except Exception:
                self.send_json(500, {'error': 'Internal error'})

    # Never expose stdlib HTTP transport or plaintext credentials beyond loopback.
    return HTTPServer(('127.0.0.1', port), Handler)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--database', required=True)
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--bootstrap', action='store_true')
    args = parser.parse_args()
    academy = Academy(args.database)
    if args.bootstrap:
        academy.bootstrap_admin(input('Administrator login: '), getpass.getpass('Administrator password: '))
        print('Administrator provisioned. No service deployed.')
        return
    server = make_server(academy, args.port)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
