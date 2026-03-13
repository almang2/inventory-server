import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const token = __ENV.TOKEN || '';
const filePath =
  __ENV.FILE_PATH ||
  '/Users/joonkyo/inventory-server/docs/fixtures/retail-upload-bulk-3000.xlsx';
const vus = Number(__ENV.VUS || 1);
const iterations = Number(__ENV.ITERATIONS || 5);
const pauseSeconds = Number(__ENV.SLEEP || 0);

if (!token) {
  throw new Error('TOKEN env is required. Example: TOKEN=<access_token> k6 run ...');
}

const uploadFile = open(filePath, 'b');
const fileName = filePath.split('/').pop() || 'retail-upload.xlsx';
const uploadDuration = new Trend('retail_upload_duration', true);

export const options = {
  scenarios: {
    retail_upload: {
      executor: 'shared-iterations',
      vus,
      iterations,
      maxDuration: '30m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<120000'],
    retail_upload_duration: ['p(95)<120000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'max'],
};

export default function () {
  const url = `${baseUrl}/api/v1/retail/upload`;
  const payload = {
    file: http.file(
      uploadFile,
      fileName,
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ),
  };

  const params = {
    headers: {
      Authorization: `Bearer ${token}`,
    },
    tags: {
      name: 'retail-upload',
    },
    timeout: '5m',
  };

  const res = http.post(url, payload, params);
  uploadDuration.add(res.timings.duration);

  let body = null;
  try {
    body = res.json();
  } catch (e) {
    body = null;
  }

  check(res, {
    'status is 200': (r) => r.status === 200,
    'response has success=true': () => body !== null && body.success === true,
  });

  if (pauseSeconds > 0) {
    sleep(pauseSeconds);
  }
}
