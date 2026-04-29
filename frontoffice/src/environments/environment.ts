/*
 To enable course quality analysis, start the Flask model first:
   cd course-model-ai
   pip install -r requirements.txt
   python train.py --seed   (first time only)
   python api.py            (starts on http://localhost:5000)

 Spring Boot proxies this through POST /api/quality/predict.
 Angular never calls localhost:5000 directly.
*/
export const environment = {
  apiGateway: 'http://localhost:8089',
  msCommunity: 'http://localhost:8089'
};
