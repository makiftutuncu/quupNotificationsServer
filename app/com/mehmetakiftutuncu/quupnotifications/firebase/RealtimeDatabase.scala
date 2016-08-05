package com.mehmetakiftutuncu.quupnotifications.firebase

import java.io.FileInputStream

import com.github.mehmetakiftutuncu.errors.{CommonError, Errors, Maybe}
import com.google.firebase.database.DatabaseReference.CompletionListener
import com.google.firebase.database._
import com.google.firebase.{FirebaseApp, FirebaseOptions}
import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.mehmetakiftutuncu.quupnotifications.utilities.ConfBase

import scala.collection.JavaConversions
import scala.concurrent.{Future, Promise}
import scala.util.Try

@Singleton
case class RealtimeDatabase @Inject()(Conf: ConfBase) extends RealtimeDatabaseBase

@ImplementedBy(classOf[RealtimeDatabase])
trait RealtimeDatabaseBase {
  protected val Conf: ConfBase

  private lazy val firebaseOptions: FirebaseOptions = new FirebaseOptions.Builder()
    .setDatabaseUrl(s"https://${Conf.RealtimeDatabase.databaseName}.firebaseio.com")
    .setServiceAccount(new FileInputStream(Conf.RealtimeDatabase.credentialsFilePath))
    .build()

  private lazy val firebaseApp: FirebaseApp           = FirebaseApp.initializeApp(firebaseOptions)
  private lazy val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance(firebaseApp)

  def get[R](path: String)(implicit manifest: Manifest[R]): Future[Maybe[R]] = {
    val promise: Promise[Maybe[R]] = Promise[Maybe[R]]()

    firebaseDatabase.getReference(path).addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(dataSnapshot: DataSnapshot): Unit = {
        val result: Maybe[R] = if (!dataSnapshot.exists()) {
          Maybe(Errors(CommonError.notFound.data(path)))
        } else {
          getValueFromDataSnapshot[R](path, dataSnapshot)
        }

        promise.success(result)
      }

      override def onCancelled(databaseError: DatabaseError): Unit = {
        promise.failure(databaseError.toException)
      }
    })

    promise.future
  }

  def getAll[R](path: String)(implicit manifest: Manifest[R]): Future[Maybe[List[R]]] = {
    val promise: Promise[Maybe[List[R]]] = Promise[Maybe[List[R]]]()

    firebaseDatabase.getReference(path).addListenerForSingleValueEvent(new ValueEventListener {
      override def onDataChange(dataSnapshot: DataSnapshot): Unit = {
        val result: Maybe[List[R]] = if (!dataSnapshot.exists()) {
          Maybe(Errors(CommonError.notFound.data(path)))
        } else {
          val dataSnapshots: List[DataSnapshot] = JavaConversions.asScalaIterator(dataSnapshot.getChildren.iterator()).toList

          dataSnapshots.foldLeft(Maybe[List[R]](Errors.empty)) {
            case (currentResult: Maybe[List[R]], dataSnapshot: DataSnapshot) =>
              if (currentResult.maybeErrors.getOrElse(Errors.empty).hasErrors) {
                currentResult
              } else {
                val maybeValue: Maybe[R] = getValueFromDataSnapshot[R](s"$path/${dataSnapshot.getKey}", dataSnapshot)

                if (maybeValue.hasErrors) {
                  Maybe(maybeValue.errors)
                } else {
                  Maybe(currentResult.maybeValue.getOrElse(List.empty[R]) :+ maybeValue.value)
                }
              }
          }
        }

        promise.success(result)
      }

      override def onCancelled(databaseError: DatabaseError): Unit = {
        promise.failure(databaseError.toException)
      }
    })

    promise.future
  }

  def set[R](path: String, value: R): Future[Errors] = {
    val promise: Promise[Errors] = Promise[Errors]()

    firebaseDatabase.getReference(path).setValue(value, new CompletionListener {
      override def onComplete(databaseError: DatabaseError, databaseReference: DatabaseReference): Unit = {
        if (databaseError != null) {
          promise.failure(databaseError.toException)
        } else {
          promise.success(Errors.empty)
        }
      }
    })

    promise.future
  }

  def delete(path: String): Future[Errors] = set(path, null)

  private def getValueFromDataSnapshot[R](path: String, dataSnapshot: DataSnapshot)(implicit manifest: Manifest[R]): Maybe[R] = {
    val klass: Class[R] = manifest.runtimeClass.asInstanceOf[Class[R]]

    val maybeValue: Option[R] = Try(dataSnapshot.getValue(klass)).toOption

    if (maybeValue.isEmpty) {
      Maybe(Errors(CommonError.invalidData.reason(s"""Cannot convert value at path "$path" to type "${klass.getSimpleName}"!""")))
    } else {
      Maybe(maybeValue.get)
    }
  }
}
