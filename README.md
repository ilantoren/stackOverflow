# Stack Overflow - 
## Mongodb what is the best way to use collection as round robin 
[see at StackOverflow](https://stackoverflow.com/questions/51214328/mongodb-what-is-the-best-way-to-use-collection-as-round-robin)

![alt explanation of problem](assets/stackoverflow.jpg)

**Description of solution**

Use an update of type [FindOneAndUpdate](https://www.mongodb.com/docs/manual/reference/method/db.collection.findAndModify/#std-label-db.collection.findAndModify-let-example) with a pipeline

**Data object:**
`{
"values" : [
"Pencil",
"Pen",
"Sharpener"
],
"selected" : "Pencil",
"counter" : 1
}`

**Pipeline:**
`
[{$project: {
values: 1,
selected: {
$arrayElemAt: [
'$values',
'$counter'
]
},
counter: {
$mod: [
{
$add: [
'$counter',
1
]
},
{
$size: '$values'
}
]
}
}}]
`
