package stackOverflow

import com.mongodb.MongoClientSettings
import com.mongodb.ReadConcern
import com.mongodb.ReadConcernLevel
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Indexes
import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.mongodb.reactivestreams.client.MongoCollection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.bson.Document
import org.bson.types.ObjectId
import java.time.Duration
import java.time.Instant
import java.util.*


/**
 * Example1  covered the cases of a small set of elements, but as the number of elements grows
 * the execution time increases as well.   Using two collections a counter and another for the elements
 * should decrease the time of execution by using two queries to accomplish the task
 */
class Example2 {
    private val readConcern = ReadConcern(  ReadConcernLevel.LINEARIZABLE)
    private val options = MongoClientSettings.builder().readConcern(readConcern).build()

    /*  use the options in a multi-server cluster */
    private val mongoClient: MongoClient = MongoClients.create(/* options */)

    private val counter: MongoCollection<Document> = mongoClient.getDatabase("test").getCollection("counter")
    private val elements: MongoCollection<Document> = mongoClient.getDatabase("test").getCollection("elements")


    val updatePipeline = mutableListOf(
        Document(
            "\$project",
            Document(
                "counter",
                Document("\$mod",
                    mutableListOf(Document("\$add",
                        listOf("\$counter", 1L)), 50001L))
            )
        )
    )

    init {
        runTest()
    }

    private fun runTest() = runBlocking {
        //set up the counter
        counter.deleteMany(Document()).asFlow().collect()
        counter.insertOne(Document( "counter", 0) ).asFlow().collect()

        // set up the elements
        elements.deleteMany(Document()).asFlow().collect()
        val buffer = mutableListOf<Document>()
        IntRange( 0,50000 ).forEach {
            elements.insertOne(Document( "name", "value $it").append( "ind", it )).asFlow().collect()
        }

        elements.createIndex(Indexes.ascending("ind")).asFlow().collect{
            log.info( "Index created $it")
        }
        IntRange(0,200000).forEach {
            launch {
                retrieveOne(it)
            }
        }
    }

    private suspend fun retrieveOne( testNumber: Int ) {
        val start = Instant.now()
        counter.findOneAndUpdate(Document(), updatePipeline).asFlow().collect{ d ->
            val key =  d["counter"]
            print( eq("ind", key))
            log.info ( "$testNumber : counter is $key")
            elements.find( eq("ind", key)).first().asFlow().collect{ d2 ->
                val timeElapsed  = Duration.between( start, Instant.now() )
                val name = d2.getString("name")
                log.info( "$testNumber  key: $key   ${timeElapsed.nano}   $name ")
            }
        }

    }


    companion object {
        val  log = LogManager.getLogger("stackoverflow")
        @JvmStatic
        fun main( args: Array<String>) {
            Example2()
        }
    }
}