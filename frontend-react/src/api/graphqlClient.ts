export type GraphQLErrorPayload = {
  message: string;
};

export type GraphQLResponse<TData> = {
  data?: TData;
  errors?: GraphQLErrorPayload[];
};

export class GraphQLRequestError extends Error {
  constructor(
    message: string,
    readonly errors: GraphQLErrorPayload[] = []
  ) {
    super(message);
    this.name = 'GraphQLRequestError';
  }
}

type GraphQLClientOptions = {
  endpoint?: string;
  getToken?: () => string | null;
  fetcher?: typeof fetch;
};

const defaultGraphQLEndpoint = import.meta.env.VITE_GRAPHQL_ENDPOINT ?? '/graphql';

export class GraphQLClient {
  private readonly endpoint: string;
  private readonly getToken: () => string | null;
  private readonly fetcher?: typeof fetch;

  constructor(options: GraphQLClientOptions = {}) {
    this.endpoint = options.endpoint ?? defaultGraphQLEndpoint;
    this.getToken = options.getToken ?? (() => localStorage.getItem('jwt_token'));
    this.fetcher = options.fetcher;
  }

  async request<TData, TVariables extends Record<string, unknown> = Record<string, never>>(
    query: string,
    variables?: TVariables
  ): Promise<TData> {
    const token = this.getToken();
    const fetcher = this.fetcher ?? fetch;
    const response = await fetcher(this.endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ query, variables })
    });

    if (!response.ok) {
      throw new GraphQLRequestError(`GraphQL request failed with HTTP ${response.status}`);
    }

    const payload = (await response.json()) as GraphQLResponse<TData>;

    if (payload.errors?.length) {
      throw new GraphQLRequestError(payload.errors.map((error) => error.message).join('; '), payload.errors);
    }

    if (!payload.data) {
      throw new GraphQLRequestError('GraphQL response did not include data');
    }

    return payload.data;
  }
}

export const graphQLClient = new GraphQLClient();
