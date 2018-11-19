import json

import boto3
import cfnresponse

def lambda_handler(event, context):
    try:
        response_status = cfnresponse.SUCCESS
        if 'RequestType' in event:
            rest_api_id = event['ResourceProperties']['RestApiId']
            stage_name = event['ResourceProperties']['StageName']

            new_value = 'false' if event['RequestType'] == 'Delete' else 'true'
            client = boto3.client('apigateway')
            client.update_stage(
                restApiId=rest_api_id,
                stageName=stage_name,
                patchOperations=[
                    {
                        'op': 'replace',
                        'path': '/tracingEnabled',
                        'value' : new_value,
                    }
                ]
            )

            cfnresponse.send(event, context, response_status, {}, '')
    except Exception:
        # Whatever happens, send the failure!
        cfnresponse.send(event, context, cfnresponse.FAILED, {}, '')
        raise