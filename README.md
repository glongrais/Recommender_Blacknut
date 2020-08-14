# RECOMMENDER BLACKNUT

## To compile the recommender :
Run `RECOMMENDER_BLACKNUT/recommender/script/binder.sh compile`

## To run the recommender :
Run `RECOMMENDER_BLACKNUT/recommender/script/binder.sh run`

## To run the recommender in check mode :
Run `RECOMMENDER_BLACKNUT/recommender/script/binder.sh run $startDate $endDate`  
`$startDate` and `$endDate`should be strings with the format dd_mm_yyyy  

This mode simulate the app running from `$startDate`to `$endDate` without making the recommendations. It will check the test config file and the queries.  

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
ABPath: ABTest.json                           /* the path and name of the A/B testing statistics results file */
nbUserPerFile: 0                              /* the number of users per result file, if set to 0 all results will be store in one file */
nbRecommendation: 5                           /* the number of recommendations per users and per algorithms */
configs:                                      /* the list of the recommendation algorithms and their config file to run for each user */
- mf/config79.yml
- ibknn/config0.yml
normalize: false                              /* normalize the data */
binarize: false                               /* binarize the data */
```

## The tests config file :

The date format is dd_mm_yyyy. Each test can have as many algos as the user wants, and does not necessarily have to be stored chronologically in JSON. The test starts on the date indicated in the "start_date" field and ends when the chronologically next test starts or when the "end_date" is reached. The keys `streamsQuery`, `gamesQuery`, `usersQuery`and `clickQuery`are optionals and can be used to specify a query for each tables.  
If `everydayRefresh` is on `true`, users will have the algorithm that will be displayed randomly assigned to each launch of the application, otherwise, if `everydayRefresh` is on `false`, they will be randomly assigned the algorithm that will be displayed on the first day of the test and will keep it until the next test or until the end of the tests.

```json
{
    "tests":[
        {
            "id":1,
            "start_date":"01_07_2020",
            "algos":["[name of algo 1]", "[name of algo 2]"],
            "everydayRefresh": false,
            "streamsQuery": "SELECT * from external_share.streams", 
            "gamesQuery": "SELECT * from external_share.games", 
            "clickQuery": "SELECT * from external_share.click", 
            "usersQuery": "SELECT * from external_share.users" 
        },
        {
            "id":2,
            "start_date":"17_07_2020",
            "algos":["[name of algo 3]", "[name of algo 4]"],
            "everydayRefresh": false
        },
        {
            "id":3,
            "start_date":"01_08_2020",
            "algos":["[name of algo 5]", "[name of algo 6]"],
            "everydayRefresh": false
        }
    ],
    "end_date":"12_12_2020"
}
```
## Recommendations Results :

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
## A/B Testing : 

The application provides A/B test statistics of the current test at each launch and stores the results in the JSON file specified in the configuration file. The results file will have this structure :  
```json
{
    "algos":["[name of algo 1]", "[name of algo 2]"],
    "nullHypothesis": false,
    "clickrate": [{"[name of algo 1]": 6.0},
                  {"[name of algo 2]": 15.0}]
}
```

## Sources : 

This project use the modifed version of Apache Mahout™ by Florestan De Moor : 
[`https://github.com/fdemoor/mahout/tree/branch-0.13.0`](https://github.com/fdemoor/mahout/tree/branch-0.13.0)

For additional information about Mahout, visit the [Mahout Home Page](http://mahout.apache.org/)
