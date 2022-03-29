package stackOverflow

import com.mongodb.ReadConcern
import com.mongodb.ReadConcernLevel
import com.mongodb.MongoClientSettings
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.bson.Document


/**
 *   Show an example of using a pipeline in a  findOneAndUpdate
 *   for the following pipeline [{$project:{values: 1, selected: {$arrayElemAt:[ "$values", "$counter"]}, counter:  {$mod:[ {$add:["$counter",1]}, {$size: "$values" }]}}} ]
 *
 */
class Example {
    private val readConcern = ReadConcern(  ReadConcernLevel.LINEARIZABLE)
    private val options = MongoClientSettings.builder().readConcern(readConcern).build()
    private val mongoClient: MongoClient = MongoClients.create(options)
    private val collection: MongoCollection<Document> = mongoClient.getDatabase("test").getCollection("counter")

    private val initialData = """
        {
        	"values" : [
        		"Pencil",
        		"Pen",
        		"Sharpener"
        	],
        	"selected" : "Pencil",
        	"counter" : 1
        }

    """.trimIndent()


     private val pipeline = mutableListOf(
         Document(
             "\$project",
             Document("values", 1L)
                 .append(
                     "selected",
                     Document("\$arrayElemAt", listOf("\$values", "\$counter"))
                 )
                 .append(
                     "counter",
                     Document(
                         "\$mod", listOf(
                             Document("\$add", listOf("\$counter", 1L)),
                             Document("\$size", "\$values")
                         )
                     )
                 )
         )
     )

    init {
        println( "sanity")
        log.info("Welcome to the test run")
    }

    private  fun runTest() = runBlocking {
        log.info( "Starting")
        val doc = Document.parse( initialData )
        // empty the collection
        collection.deleteMany(Document()).asFlow().collect{
            log.info( "deleted ${it.deletedCount}")
        }
        // initialize with data
        collection.insertOne( doc).asFlow().collect{
            log.info( "Inserted ${it.insertedId}")
        }
        IntRange(0,1000).forEach { r ->
            launch {
                singleUpdate(r)
            }
        }
    }

    private suspend fun singleUpdate( run: Int) {
        collection.findOneAndUpdate(Document(), pipeline).asFlow().collect {
            val selected = it.getString( "selected")
           log.info( "$run   $selected ")
        }
    }


    companion object  {
        val  log = LogManager.getLogger("stackoverflow")
        @JvmStatic
        fun main ( args : Array<String>) {
            Example().runTest()
        }
    }
}