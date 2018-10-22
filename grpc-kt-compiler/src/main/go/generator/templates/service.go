package templates

const (
	Service = `
{{- with $s := .}}
package {{$s.JavaPackage}}

import kotlinx.coroutines.channels.ReceiveChannel
import io.grpc.MethodDescriptor.generateFullMethodName
import io.grpc.kt.stub.ClientCallsKt
import io.grpc.kt.stub.ServerCallsKt

{{- /**
 * <pre>
 * Test service that supports all call types.
 * </pre>
 */}}
@javax.annotation.Generated(
  value = ["by gRPC proto compiler (version 0.5.0)"],
  comments = "Source: {{.ProtoFile}}")
object {{$s.Name}}GrpcKt {
  const val SERVICE_NAME = "{{$s.ProtoName}}"

  // Static method descriptors that strictly reflect the proto.
  {{- range $i, $m := .Methods}}
  @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
  val {{$m.FieldName}}: io.grpc.MethodDescriptor<{{$m.InputType}}, {{$m.OutputType}}> =
    io.grpc.MethodDescriptor.newBuilder<{{$m.InputType}}, {{$m.OutputType}}>()
      .setType(io.grpc.MethodDescriptor.MethodType.{{$m.GrpcMethodType}})
      .setFullMethodName(generateFullMethodName("{{$s.ProtoName}}", "{{$m.Name}}"))
      .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller({{$m.InputType}}.getDefaultInstance()))
      .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller({{$m.OutputType}}.getDefaultInstance()))
      .build()
  {{- end}}

  /**
   * Creates a new RX stub
   */
  fun newStub(channel: io.grpc.Channel): {{.Name}}Stub {
    return {{.Name}}Stub(channel)
  }

  /**
   * Creates a new RX stub with call options
   */
  fun newStub(channel: io.grpc.Channel, callOptions: io.grpc.CallOptions): {{.Name}}Stub {
    return {{.Name}}Stub(channel, callOptions)
  }

  {{/**
   * <pre>
   * Test service that supports all call types.
   * </pre>
   */}}
  abstract class {{.Name}}ImplBase : io.grpc.BindableService {

    {{range $i, $m := .Methods}}
    {{- /**
     * <pre>
     * One requestMore followed by one response.
     * The server returns the client payload as-is.
     * </pre>
     */ -}}
    open suspend fun {{$m.JavaName}}(req: {{$m.FullInputType}}): {{$m.FullOutputType}} {
      return ServerCallsKt.{{$m.UnimplementedCall}}({{$m.FieldName}})
    }
    {{end}}

    override fun bindService(): io.grpc.ServerServiceDefinition {
      return io.grpc.ServerServiceDefinition.builder(serviceDescriptor)
        {{- range $i, $m := .Methods}}
        .addMethod(
          {{$m.FieldName}},
          ServerCallsKt.{{$m.Call}}(
            MethodHandlers<
              {{$m.InputType}},
              {{$m.OutputType}}>(
              this, {{$m.IdName}})))
        {{- end}}
        .build()
    }
  }

  {{/**
   * <pre>
   * Test service that supports all call types.
   * </pre>
   */}}
  class {{$s.Name}}Stub internal constructor(channel: io.grpc.Channel, callOptions: io.grpc.CallOptions)
    : io.grpc.stub.AbstractStub<{{$s.Name}}Stub>(channel, callOptions) {
    internal constructor(channel: io.grpc.Channel): this(channel, io.grpc.CallOptions.DEFAULT)

    override fun build(channel: io.grpc.Channel,
                    callOptions: io.grpc.CallOptions): {{$s.Name}}Stub {
      return {{$s.Name}}Stub(channel, callOptions)
    }

    {{range $i, $m := .Methods}}
    {{- /**
     * <pre>
     * One requestMore followed by one response.
     * The server returns the client payload as-is.
     * </pre>
     */ -}}
    suspend fun {{$m.JavaName}}(req: {{$m.FullInputType}}): {{$m.FullOutputType}} {
      return ClientCallsKt.{{$m.Call}}(
        getChannel().newCall({{$m.FieldName}}, callOptions), {{$m.CallParams}});
    }
    {{end}}
  }

  {{range $i, $m := .Methods}}
  const val {{$m.IdName}} = {{$m.Id}};
  {{- end}}

  private class MethodHandlers<REQ, RESP>(private val serviceImpl: {{$s.Name}}ImplBase, private val methodId: Int) :
    ServerCallsKt.UnaryMethod<REQ, RESP>,
    ServerCallsKt.ServerStreamingMethod<REQ, RESP>,
    ServerCallsKt.ClientStreamingMethod<REQ, RESP>,
    ServerCallsKt.BidiStreamingMethod<REQ, RESP> {

    @Suppress("UNCHECKED_CAST")
    override suspend fun unaryInvoke(req: REQ): RESP {
      return when (methodId) {
        {{- range $i, $m := .Methods}}
        {{- if eq $m.MethodType 0}}
        {{$m.IdName}} -> serviceImpl.{{$m.JavaName}}(req as {{$m.FullInputType}}) as RESP
        {{- end}}
        {{- end}}
        else -> throw AssertionError()
      }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun serverStreamingInvoke(req: REQ): ReceiveChannel<RESP> {
      return when (methodId) {
        {{- range $i, $m := .Methods}}
        {{- if eq $m.MethodType 1}}
        {{$m.IdName}} -> serviceImpl.{{$m.JavaName}}(req as {{$m.FullInputType}}) as ReceiveChannel<RESP>
        {{- end}}
        {{- end}}
        else -> throw AssertionError()
      }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun clientStreamingInvoke(req: ReceiveChannel<REQ>): RESP {
      return when (methodId) {
        {{- range $i, $m := .Methods}}
        {{- if eq $m.MethodType 2}}
        {{$m.IdName}} -> serviceImpl.{{$m.JavaName}}(req as {{$m.FullInputType}}) as RESP
        {{- end}}
        {{- end}}
        else -> throw AssertionError()
      }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun bidiStreamingInvoke(req: ReceiveChannel<REQ>): ReceiveChannel<RESP> {
      return when (methodId) {
        {{- range $i, $m := .Methods}}
        {{- if eq $m.MethodType 3}}
        {{$m.IdName}} -> serviceImpl.{{$m.JavaName}}(req as {{$m.FullInputType}}) as ReceiveChannel<RESP>
        {{- end}}
        {{- end}}
        else -> throw AssertionError()
      }
    }

  }

  private class {{$s.Name}}DescriptorSupplier : io.grpc.protobuf.ProtoFileDescriptorSupplier {
    override fun getFileDescriptor(): com.google.protobuf.Descriptors.FileDescriptor {
      return {{$s.JavaPackage}}.{{$s.OuterClassName}}.getDescriptor();
    }
  }

  val serviceDescriptor: io.grpc.ServiceDescriptor by lazy {
    io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
      .setSchemaDescriptor({{$s.Name}}DescriptorSupplier())
      {{- range $i, $m := .Methods}}
      .addMethod({{$m.FieldName}})
      {{- end}}
      .build()
  }
}
{{- end}}
`
)
