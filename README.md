# RECOMMENDER BLACKNUT

## To compile the recommender :
Run `RECOMMENDER_BLACKNUT/recommender/script/binder.sh compile`

## To run the recommender :
Run `RECOMMENDER_BLACKNUT/recommender/script/binder.sh run`

## To change the setting of the recommender : 
Edit `RECOMMENDER_BLACKNUT/recommender/app/src/main/ressources/default_config.yml`  
Or create a new config file and in `RECOMMENDER_BLACKNUT/recommender/app/run.sh` uncomment `--config="$CONFIG` and indicate the path in `CONFIG="..."`

## The config file : 
```java
data: local                                   /* local | online */
dataset: src/main/resources/streams.csv       /* if data is set to local, this indicates the path where to collect the datas */
resultPath: result.json                       /* the path and the name of the result file */
keyPath: blacknut-analytics-7488e0e6d73d.json /* if data is set to online, this indicates the path and the name of the key file to access to the BigQuery datas */
testPath: tests.json                          /* the path and name of the tests config file */
nbUserPerFile: 0                              /* the number of users per result file, if set to 0 all results will be store in one file */
nbRecommendation: 5                           /* the number of recommendations per users and per algorithms */
configs:                                      /* the list of the recommendation algorithms and their config file to run for each user */
- mf/config79.yml
- ibknn/config0.yml
normalize: false                              /* normalize the data */
binarize: false                               /* binarize the data */
```

## The tests config file :

The date format is dd_mm_yyyy. Each test can have as many algos as the user wants, and does not necessarily have to be stored chronologically in JSON. The test starts on the date indicated in the "start_date" field and ends when the chronologically next test starts or when the "end_date" is reached. 

```json
{
    "tests":[
        {
            "id":1,
            "start_date":"01_07_2020",
            "algos":["[name of algo 1]", "[name of algo 2]"]
        },
        {
            "id":2,
            "start_date":"17_07_2020",
            "algos":["[name of algo 3]", "[name of algo 4]"]
        },
        {
            "id":3,
            "start_date":"01_08_2020",
            "algos":["[name of algo 5]", "[name of algo 6]"]
        }
    ],
    "end_date":"12_12_2020"
}
```
## Results :

The application will return the results as one or more JSON files. The results will be stored with this structure : 
```json
[
    {
        "user_id": "1",
        "display": "[name of algo 1]",
        "[name of algo 1]": [
            "4",
            "24",
            "5"
        ],
        "[name of algo 2]": [
            "12",
            "4",
            "5"
        ],
        "test_id": 1
    },
    {
        "user_id": "2",
        "display": "[name of algo 2]",
        "[name of algo 1]": [
            "7",
            "4",
            "45"
        ],
        "[name of algo 2]": [
            "21",
            "9",
            "7"
        ],
        "test_id": 1
    },
]
```
## Sources : 

This project use the modifed version of Apache Mahout™ by Florestan De Moor : 
[`https://github.com/fdemoor/mahout/tree/branch-0.13.0`](https://github.com/fdemoor/mahout/tree/branch-0.13.0)

For additional information about Mahout, visit the [Mahout Home Page](http://mahout.apache.org/)
